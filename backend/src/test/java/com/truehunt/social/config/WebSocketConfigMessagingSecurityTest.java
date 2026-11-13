package com.truehunt.social.config;

import com.truehunt.social.security.CustomUserDetailsService;
import com.truehunt.social.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketConfigMessagingSecurityTest {

    private JwtTokenProvider jwtTokenProvider;
    private CustomUserDetailsService userDetailsService;
    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        interceptor = new WebSocketConfig(jwtTokenProvider, userDetailsService)
                .authenticatedMessagingInterceptor();
    }

    @Test
    void rejectsConnectionsWithoutAValidJwt() {
        assertNull(send(StompCommand.CONNECT, null, null, null));
    }

    @Test
    void allowsOnlyPrivateMessageSubscriptionsAndApplicationSends() {
        var authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(
                "person@example.com",
                null,
                AuthorityUtils.NO_AUTHORITIES
        );

        assertNotNull(send(StompCommand.SUBSCRIBE, "/user/queue/messages", authenticatedUser, null));
        assertNull(send(StompCommand.SUBSCRIBE, "/topic/messages", authenticatedUser, null));
        assertNull(send(StompCommand.SEND, "/topic/messages", authenticatedUser, null));
        assertNotNull(send(StompCommand.SEND, "/app/chat.private", authenticatedUser, null));
    }

    @Test
    void authenticatesConnectionsUsingOnlyTheJwtPrincipal() {
        UserDetails user = mock(UserDetails.class);
        doReturn(AuthorityUtils.NO_AUTHORITIES).when(user).getAuthorities();
        when(user.getUsername()).thenReturn("person@example.com");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("person@example.com");
        when(userDetailsService.loadUserByUsername("person@example.com")).thenReturn(user);

        assertNotNull(send(StompCommand.CONNECT, null, null, "valid-token"));
    }

    private Message<?> send(StompCommand command, String destination,
                            UsernamePasswordAuthenticationToken user, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (user != null) accessor.setUser(user);
        if (token != null) accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return interceptor.preSend(message, mock(MessageChannel.class));
    }
}
