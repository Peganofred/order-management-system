package com.portfolio.orderms.controller;

import com.portfolio.orderms.dto.UserResponse;
import com.portfolio.orderms.entity.User;
import com.portfolio.orderms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * @AuthenticationPrincipal binds to Authentication.getPrincipal(), which
     * JwtAuthenticationFilter set to the plain email String - not a UserDetails
     * object - since request-time auth never touches the DB. Hence String here,
     * not our User entity or a UserDetails type.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
