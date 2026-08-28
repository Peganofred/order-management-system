package com.portfolio.orderms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request, before Spring Security's own auth filter.
 * Trusts the role claim baked into the JWT and builds the Authentication
 * object directly from it — no DB lookup here. That's what keeps request
 * handling stateless/fast, at the cost of role changes not applying until
 * the user's current token expires and they log in again.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);

                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // Invalid/expired token: SecurityContext stays empty. We don't reject the
            // request here - public endpoints (e.g. /api/v1/auth/**) must keep working
            // even with a stale token attached. authorizeHttpRequests() decides whether
            // the specific route actually requires authentication.
        }

        filterChain.doFilter(request, response);
    }
}
