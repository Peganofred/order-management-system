package com.portfolio.orderms.entity;

/**
 * Domain roles. Deliberately NOT prefixed with "ROLE_" here — that prefix is
 * a Spring Security convention (GrantedAuthority.getAuthority()), not part of
 * our domain model or DB data. We add "ROLE_" only at the point where we build
 * a GrantedAuthority (CustomUserDetailsService, JwtAuthenticationFilter).
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
