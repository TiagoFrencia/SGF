package com.sgf.modules.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgf.audit.service.AuditService;
import com.sgf.modules.auth.domain.UserAccount;
import com.sgf.modules.auth.domain.UserAccountRepository;
import com.sgf.modules.auth.web.AuthResponse;
import com.sgf.modules.auth.web.LoginRequest;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userAccountRepository, jwtService, auditService);
    }

    @Test
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("tiago", "password123");
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setUsername("tiago");
        user.setActive(true);
        user.setRoles(Collections.emptySet());

        when(userAccountRepository.findByUsernameAndActiveTrue("tiago")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("mocked-jwt-token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.accessToken());
        assertEquals("tiago", response.username());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(auditService).record(eq("tiago"), eq("LOGIN"), any(), any(), any());
    }

    @Test
    void shouldThrowException_WhenCredentialsInvalid() {
        // Given
        LoginRequest request = new LoginRequest("user", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userAccountRepository, never()).findByUsernameAndActiveTrue(any());
    }

    @Test
    void shouldThrowException_WhenUserNotFoundAfterAuth() {
        // Given
        LoginRequest request = new LoginRequest("phantom", "pass");
        when(userAccountRepository.findByUsernameAndActiveTrue("phantom")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(java.util.NoSuchElementException.class, () -> authService.login(request));
    }
}
