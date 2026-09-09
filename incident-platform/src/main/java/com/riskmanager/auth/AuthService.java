package com.riskmanager.auth;

import com.riskmanager.exception.BadRequestException;
import com.riskmanager.exception.ResourceNotFoundException;
import com.riskmanager.security.JwtService;
import com.riskmanager.user.Role;
import com.riskmanager.user.User;
import com.riskmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : Role.ENGINEER)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email: {} and role: {}", savedUser.getEmail(), savedUser.getRole());

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(AuthRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedEmail));

        if (!user.isActive()) {
            throw new BadRequestException("User account is deactivated. Contact an administrator.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        log.info("User logged in successfully: {}", user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
