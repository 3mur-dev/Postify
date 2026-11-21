package com.omar.postify.security;

import com.omar.postify.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Set<String> ALLOWED_SUBSCRIPTIONS = Set.of(
            "/user/queue/messages",
            "/user/queue/notifications",
            "/user/queue/presence"
    );

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(
            @Nonnull Message<?> message,
            @Nonnull MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken =
                    accessor.getFirstNativeHeader("Authorization");

            if (bearerToken == null ||
                    !bearerToken.startsWith("Bearer ")) {

                throw new IllegalArgumentException(
                        "Missing WebSocket Authorization header"
                );
            }

            String token = bearerToken.substring(7);

            try {
                String username = jwtUtil.extractUsername(token);

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled() || !jwtUtil.validateToken(token, userDetails)) {
                    throw new IllegalArgumentException(
                            "Invalid WebSocket JWT"
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                accessor.setUser(authentication);
            } catch (JwtException ex) {
                /*
                 * Only catch JwtException here. IllegalArgumentException thrown
                 * by the code above (e.g., "Invalid WebSocket JWT") propagates
                 * with its original message intact, instead of being wrapped
                 * into a generic "WebSocket authentication failed" message.
                 */
                throw new IllegalArgumentException(
                        "WebSocket authentication failed",
                        ex
                );
            }
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            requireActiveUser(accessor);
            if (!Set.of("/app/chat.send", "/app/chat.presence")
                    .contains(accessor.getDestination())) {
                throw new IllegalArgumentException("Unsupported messaging destination");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            requireActiveUser(accessor);
            if (!ALLOWED_SUBSCRIPTIONS.contains(accessor.getDestination())) {
                throw new IllegalArgumentException("Unsupported subscription destination");
            }
        }

        return message;
    }

    private void requireAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Authenticated connection required");
        }
    }

    private void requireActiveUser(StompHeaderAccessor accessor) {
        requireAuthenticated(accessor);
        UserDetails currentUser =
                userDetailsService.loadUserByUsername(accessor.getUser().getName());
        if (!currentUser.isEnabled()) {
            throw new IllegalArgumentException("Account is not active");
        }
    }
}