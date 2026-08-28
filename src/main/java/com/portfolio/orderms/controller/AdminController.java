package com.portfolio.orderms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo-only endpoint that exists purely to prove RBAC works end-to-end:
 * CUSTOMER token -> 403, ADMIN token -> 200. Real admin functionality
 * (managing products, viewing all orders, etc.) gets added in later phases
 * once those domains exist.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> ping() {
        return Map.of("message", "pong - you are an ADMIN");
    }
}
