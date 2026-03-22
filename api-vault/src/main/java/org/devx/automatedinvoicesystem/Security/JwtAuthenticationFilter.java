package org.devx.automatedinvoicesystem.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Standard constructor injection (Can also use Lombok's @RequiredArgsConstructor )
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        // Grab the Authorization header from the incoming request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // If there is no header, or it doesn't start with "Bearer",
        // we pass the request down the chain. It might be a public endpoint (like /api/v1/auth/login).
        // If it's not public, the next security layer will block it.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Slice off "Bearer " (7 characters) to get the pure token
        jwt = authHeader.substring(7);

        // Extract the email using the cryptographic engine we built in JwtService
        userEmail = jwtService.extractUsername(jwt);

        // If we found an email, AND this specific thread hasn't been authenticated yet...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Go to PostgreSQL and fetch the user's current data (to ensure they weren't deleted)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Does the math check out or is it unexpired?
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 6. THE STAMP OF APPROVAL: Create the Spring Security authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // We don't need credentials (password) here because the JWT proved who they are
                        userDetails.getAuthorities()
                );

                // Attach the details of the incoming web request (like IP address, session ID)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Update the ThreadLocal context. This user is now officially logged in for this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Pass the fully authenticated request to the Controller
        filterChain.doFilter(request, response);
    }

}