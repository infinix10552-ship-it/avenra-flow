package org.devx.automatedinvoicesystem.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. DISABLE CSRF: Cross-Site Request Forgery protection is for Cookie-based apps.
                // We are using stateless JWTs in headers, so CSRF is dead weight.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. ENABLE CORS: Tell Spring Security to respect the @CrossOrigin annotations on our Controllers
                .cors(Customizer.withDefaults())

                // 3. ROUTE PERMISSIONS: The VIP List vs. The Vault
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // The Authentication Controller MUST be public so users can get a token
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Let the HTTP upgrade request through
                        .requestMatchers("/ws/**").permitAll()
                        // Whitelist our local S3 simulation so Python can download the PDFs
                        .requestMatchers("/local-files/**").permitAll()
                        // Allow Python to talk to Java without a JWT
                        .requestMatchers("/api/v1/webhook/**").permitAll()
                        // Health checks should be public so load balancers can ping them
                        .requestMatchers("/health").permitAll()
                        // OAuth2 callback paths must be public (prevents JWT filter from intercepting Google's redirect)
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        // Any request to core APIs MUST be authenticated
                        .requestMatchers("/api/v1/invoices/**").authenticated()
                        .requestMatchers("/api/v1/clients/**").authenticated()
                        // Catch-all: Anything else must be authenticated
                        .anyRequest().authenticated()
                )

                // 4. SESSION POLICY: IF_REQUIRED allows the temporary session needed by
                // Spring's OAuth2 authorization code flow (Google redirect needs state correlation),
                // while the JWT filter ensures all API requests are still fully stateless.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler) // Wire our custom bridge
                )

                // 5. WIRE THE ENGINES: Attach the Identity Manager that was built in ApplicationConfig
                .authenticationProvider(authenticationProvider)

                // 6. DEPLOY THE GUARD: Insert our custom JWT Filter BEFORE the default Spring password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
