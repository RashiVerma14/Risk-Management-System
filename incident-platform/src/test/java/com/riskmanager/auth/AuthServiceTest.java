package com.riskmanager.auth;

import com.riskmanager.exception.BadRequestException;
import com.riskmanager.exception.ResourceNotFoundException;
import com.riskmanager.security.JwtService;
import com.riskmanager.user.Role;
import com.riskmanager.user.User;
import com.riskmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("u1")
                .name("Alex SRE")
                .email("alex@riskmanager.io")
                .password("encoded_pass")
                .role(Role.ENGINEER)
                .active(true)
                .build();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("Alex SRE", "alex@riskmanager.io", "pass123", Role.ENGINEER);

        when(userRepository.existsByEmail("alex@riskmanager.io")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken("alex@riskmanager.io", "ENGINEER")).thenReturn("mock-jwt-token");

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("mock-jwt-token", resp.getToken());
        assertEquals("alex@riskmanager.io", resp.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsBadRequest() {
        RegisterRequest req = new RegisterRequest("Alex SRE", "alex@riskmanager.io", "pass123", Role.ENGINEER);

        when(userRepository.existsByEmail("alex@riskmanager.io")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest req = new AuthRequest("alex@riskmanager.io", "pass123");

        when(userRepository.findByEmail("alex@riskmanager.io")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken("alex@riskmanager.io", "ENGINEER")).thenReturn("mock-jwt-token");

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock-jwt-token", resp.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginInactiveUserThrowsBadRequest() {
        sampleUser.setActive(false);
        AuthRequest req = new AuthRequest("alex@riskmanager.io", "pass123");

        when(userRepository.findByEmail("alex@riskmanager.io")).thenReturn(Optional.of(sampleUser));

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    void testLoginWrongPasswordThrowsBadCredentials() {
        AuthRequest req = new AuthRequest("alex@riskmanager.io", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
