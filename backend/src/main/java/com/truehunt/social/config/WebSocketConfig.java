package com.truehunt.social.config;

import com.truehunt.social.security.CustomUserDetailsService;
import com.truehunt.social.security.JwtTokenProvider;
import com.truehunt.social.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket endpoint for standard STOMP clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // SockJS fallback endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable in-memory broker for public topics and private user queues
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages routed to @MessageMapping handlers
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific private messages (/user/{username}/queue/...)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String userEmail = null;

                    // 1. Check Authorization header
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authHeader)) {
                        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
                        if (jwtTokenProvider.validateToken(token)) {
                            userEmail = jwtTokenProvider.getEmailFromToken(token);
                        }
                    }

                    // 2. Check X-User-Email or user header
                    if (!StringUtils.hasText(userEmail)) {
                        String headerEmail = accessor.getFirstNativeHeader("X-User-Email");
                        if (!StringUtils.hasText(headerEmail)) {
                            headerEmail = accessor.getFirstNativeHeader("user");
                        }
                        if (StringUtils.hasText(headerEmail)) {
                            userEmail = headerEmail.trim();
                        }
                    }

                    // 3. Fallback to passcode if passed as login/passcode in STOMP connect
                    if (!StringUtils.hasText(userEmail) && StringUtils.hasText(accessor.getLogin())) {
                        userEmail = accessor.getLogin();
                    }

                    if (StringUtils.hasText(userEmail)) {
                        try {
                            UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            accessor.setUser(authentication);
                            log.info("[WebSocket] Authenticated STOMP session for principal: {}", authentication.getName());
                        } catch (Exception e) {
                            // Guest or ad-hoc user principal fallback
                            final String finalUser = userEmail;
                            Principal principal = () -> finalUser;
                            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
                            log.info("[WebSocket] Ad-hoc STOMP session for user: {}", userEmail);
                        }
                    }
                }
                return message;
            }
        });
    }
}

