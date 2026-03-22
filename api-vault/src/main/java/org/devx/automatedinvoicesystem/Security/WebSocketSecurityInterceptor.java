// This class is intended to be a WebSocket interceptor that checks for authentication tokens in the WebSocket handshake request.
// It will validate the token and ensure that only authenticated users can establish a WebSocket connection.


package org.devx.automatedinvoicesystem.Security;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // Ensure this runs before Spring's internal routing
public class WebSocketSecurityInterceptor implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketSecurityInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // ONLY check security when the user is initially trying to connect
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // Extract the authorization header from the STOMP payload
                    List<String> authorization = accessor.getNativeHeader("Authorization");

                    if (authorization != null && !authorization.isEmpty()) {
                        String authHeader = authorization.get(0);
                        if (authHeader.startsWith("Bearer ")) {
                            String jwt = authHeader.substring(7);
                            String userEmail = jwtService.extractUsername(jwt);

                            if (userEmail != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                                // Validate the math
                                if (jwtService.isTokenValid(jwt, userDetails)) {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                                    // Officially authenticate the WebSocket session
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                    accessor.setUser(authentication);
                                } else {
                                    throw new IllegalArgumentException("Invalid JWT Token for WebSocket connection");
                                }
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Missing JWT Token in WebSocket connection");
                    }
                }
                return message;
            }
        });
    }
}
