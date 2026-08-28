package com.portfolio.orderms.config;

import com.portfolio.orderms.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Design decisions worth defending in an interview:
 *
 * - CSRF disabled: CSRF protects session-cookie based auth from a browser
 *   silently replaying a user's cookie. We're stateless/token-based (the
 *   token lives in an Authorization header the browser doesn't auto-attach),
 *   so the attack CSRF defends against doesn't apply here.
 *
 * - SessionCreationPolicy.STATELESS: no HttpSession is created or read.
 *   Every request must carry its own proof of identity (the JWT). This is
 *   what lets the API scale horizontally without sticky sessions.
 *
 * - Role checks mostly live as @PreAuthorize on individual controller
 *   methods (see AdminController) rather than as a long list of
 *   .requestMatchers(...).hasRole(...) here. Method-level annotations keep
 *   the authorization rule next to the code it protects instead of a
 *   growing, easy-to-forget list in one config class.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // turns on @PreAuthorize/@PostAuthorize on beans (see AdminController)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Catalog browsing (Phase 3) needs no account, same as any
                        // storefront. Only GET is opened up here - POST/PUT/DELETE
                        // on the same /api/v1/products path still fall through to
                        // anyRequest().authenticated() below, gated further by
                        // @PreAuthorize("hasRole('ADMIN')") on those controller
                        // methods (see ProductController).
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // No token / bad token on a protected route -> clean 401 JSON
                        // instead of Spring's default whitelabel HTML error page.
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid token\"}");
                        })
                        // Valid token, but wrong role for this endpoint -> 403, not 401.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"error\":\"Forbidden\",\"message\":\"You don't have permission to access this resource\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
