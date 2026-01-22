package com.st3.uber.config;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                try {
                    Jwt jwt = jwtDecoder.decode(token);

                    // Extract userId from JWT claims
                    Long userId = jwt.getClaim("uid");
                    String role = jwt.getClaim("role");

                    System.out.println("🔐 WebSocket Auth - User ID: " + userId + ", Role: " + role);

                    if (userId == null) {
                        System.err.println("❌ No 'uid' claim found in JWT token!");
                        return message;
                    }

                    // Create authentication with userId as the name
                    // THIS IS CRITICAL: The name MUST match what convertAndSendToUser uses
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(),  // Use userId.toString() as the principal name
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

                    // Set user with userId as the name
                    accessor.setUser(new Principal() {
                        @Override
                        public String getName() {
                            return userId.toString(); // CRITICAL: Return userId as String
                        }
                    });

                    System.out.println("✅ WebSocket user authenticated: " + userId);

                } catch (Exception e) {
                    System.err.println("❌ JWT validation failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("❌ No Authorization header found in WebSocket CONNECT");
            }
        }

        return message;
    }
}