package com.omar.postify.security;

import com.omar.postify.entities.CustomUserDetails;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final WebSocketAuthInterceptor interceptor =
            new WebSocketAuthInterceptor(jwtUtil, userDetailsService);
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUpActiveUser() {
        when(userDetailsService.loadUserByUsername("user"))
                .thenReturn(new CustomUserDetails(user("user", UserStatus.ACTIVE)));
    }

    @Test
    void rejectsSubscriptionToAnotherUsersPrivateQueue() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/user/another-user/queue/messages",
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void allowsAuthenticatedPrivateMessageSubscription() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/messages",
                true
        );

        assertDoesNotThrow(() -> interceptor.preSend(message, channel));
    }

    @Test
    void allowsAuthenticatedPrivatePresenceSubscription() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/presence",
                true
        );

        assertDoesNotThrow(() -> interceptor.preSend(message, channel));
    }

    @Test
    void rejectsUnapprovedSubscriptions() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/presence",
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void rejectsUnauthenticatedMessagePublish() {
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/app/chat.send",
                false
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void rejectsJwtForModeratedAccountDuringConnect() {
        User user = user("moderated-user", UserStatus.BANNED);
        when(jwtUtil.extractUsername("token")).thenReturn("moderated-user");
        when(userDetailsService.loadUserByUsername("moderated-user"))
                .thenReturn(new CustomUserDetails(user));

        Message<?> message = stompConnectMessage("Bearer token");

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void rejectsExistingSocketAfterAccountIsModerated() {
        User suspendedUser = user("user", UserStatus.SUSPENDED);
        when(userDetailsService.loadUserByUsername("user"))
                .thenReturn(new CustomUserDetails(suspendedUser));
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/app/chat.send",
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    private Message<?> stompConnectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader(
                "Authorization",
                authorization.startsWith("Bearer ") ? authorization : "Bearer token"
        );
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private User user(String username, UserStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setStatus(status);
        return user;
    }

    private Message<?> stompMessage(
            StompCommand command,
            String destination,
            boolean authenticated
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (authenticated) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    "user",
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER")
            ));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
