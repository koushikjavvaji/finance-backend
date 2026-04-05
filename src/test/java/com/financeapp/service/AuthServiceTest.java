package com.financeapp.service;

import com.financeapp.dto.request.AuthRequest;
import com.financeapp.dto.response.AuthResponse;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.exception.BadRequestException;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtUtils;
import com.financeapp.service.impl.AuthServiceImpl;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils              jwtUtils;

    @InjectMocks AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@finance.com")
                .password("hashed_password")
                .role(Role.VIEWER)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_withNewEmail_createsViewerAndReturnsToken() {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setName("Test User");
        req.setEmail("test@finance.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("test@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtils.generateToken("test@finance.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRole()).isEqualTo(Role.VIEWER);
        assertThat(response.getEmail()).isEqualTo("test@finance.com");

        verify(userRepository).save(argThat(u ->
                u.getRole() == Role.VIEWER &&
                u.getEmail().equals("test@finance.com")
        ));
    }

    @Test
    void register_withDuplicateEmail_throwsBadRequestException() {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setName("Test User");
        req.setEmail("test@finance.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("test@finance.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returnsTokenAndCorrectRole() {
        AuthRequest.Login req = new AuthRequest.Login();
        req.setEmail("test@finance.com");
        req.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("test@finance.com", null));
        when(userRepository.findByEmail("test@finance.com")).thenReturn(Optional.of(sampleUser));
        when(jwtUtils.generateToken("test@finance.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRole()).isEqualTo(Role.VIEWER);
        assertThat(response.getName()).isEqualTo("Test User");
    }

    @Test
    void login_withWrongCredentials_throwsException() {
        AuthRequest.Login req = new AuthRequest.Login();
        req.setEmail("test@finance.com");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtils, never()).generateToken(any());
    }
}
