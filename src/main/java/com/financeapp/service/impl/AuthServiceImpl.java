package com.financeapp.service.impl;

import com.financeapp.dto.request.AuthRequest;
import com.financeapp.dto.response.AuthResponse;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.exception.BadRequestException;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtUtils;
import com.financeapp.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils             jwtUtils;

    @Override
    public AuthResponse register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.VIEWER)   // self-registered users start as VIEWER; admin can elevate
                .build();

        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getEmail());
        return buildResponse(user, token);
    }

    @Override
    public AuthResponse login(AuthRequest.Login request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtils.generateToken(user.getEmail());
        return buildResponse(user, token);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
