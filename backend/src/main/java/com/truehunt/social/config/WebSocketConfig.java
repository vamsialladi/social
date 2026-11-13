package com.truehunt.social.config;

import com.truehunt.social.security.CustomUserDetailsService;
import com.truehunt.social.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticatedMessagingInterceptor());
    }

    ChannelInterceptor authenticatedMessagingInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (!StringUtils.hasText(authHeader)) {
                        log.warn("[WebSocket] Rejected STOMP connection without a bearer token.");
                        return null;
                    }

                    String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
                    if (!jwtTokenProvider.validateToken(token)) {
                        log.warn("[WebSocket] Rejected STOMP connection with an invalid bearer token.");
                        return null;
                    }

                    String userEmail = jwtTokenProvider.getEmailFromToken(token);
                    if (!StringUtils.hasText(userEmail)) {
                        log.warn("[WebSocket] Rejected STOMP connection without a token identity.");
                        return null;
                    }

                    try {
                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                        log.info("[WebSocket] Authenticated STOMP session for principal: {}", authentication.getName());
                    } catch (Exception e) {
                        log.warn("[WebSocket] Rejected STOMP connection for unknown principal: {}", userEmail);
                        return null;
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Authentication authentication = accessor.getUser() instanceof Authentication
                            ? (Authentication) accessor.getUser()
                            : null;
                    String destination = accessor.getDestination();
                    if (authentication == null || !authentication.isAuthenticated()
                            || !isAllowedSubscription(destination)) {
                        log.warn("[WebSocket] Rejected unauthorized STOMP subscription: {}", destination);
                        return null;
                    }
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    Authentication authentication = accessor.getUser() instanceof Authentication
                            ? (Authentication) accessor.getUser()
                            : null;
                    String destination = accessor.getDestination();
                    if (authentication == null || !authentication.isAuthenticated()
                            || !isAllowedSendDestination(destination)) {
                        log.warn("[WebSocket] Rejected unauthorized STOMP send: {}", destination);
                        return null;
                    }
                }

                return message;
            }

            private boolean isAllowedSubscription(String destination) {
                return "/user/queue/messages".equals(destination)
                        || "/topic/connections".equals(destination)
                        || (destination != null && destination.matches("/topic/user\\.[^/]+\\.connections"));
            }

            private boolean isAllowedSendDestination(String destination) {
                return "/app/chat.send".equals(destination)
                        || "/app/chat.private".equals(destination);
            }
        };
    }
}
