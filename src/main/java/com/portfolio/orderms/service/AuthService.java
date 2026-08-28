package com.portfolio.orderms.service;

import com.portfolio.orderms.dto.AuthResponse;
import com.portfolio.orderms.dto.LoginRequest;
import com.portfolio.orderms.dto.RegisterRequest;
import com.portfolio.orderms.entity.Role;
import com.portfolio.orderms.entity.User;
import com.portfolio.orderms.exception.DuplicateEmailException;
import com.portfolio.orderms.exception.InvalidCredentialsException;
import com.portfolio.orderms.repository.UserRepository;
import com.portfolio.orderms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // Role always defaults to CUSTOMER. RegisterRequest has no role field,
        // so there's nothing for a client to tamper with here even if they tried.
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException | DisabledException ex) {
            // BadCredentialsException covers "no such user" too, since
            // CustomUserDetailsService's UsernameNotFoundException gets
            // wrapped into BadCredentialsException by ProviderManager
            // (when hideUserNotFoundExceptions, the default, is true).
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
