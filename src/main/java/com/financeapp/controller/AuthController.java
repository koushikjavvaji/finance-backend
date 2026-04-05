package com.financeapp.controller;

import com.financeapp.dto.request.AuthRequest;
import com.financeapp.dto.response.ApiResponse;
import com.financeapp.dto.response.AuthResponse;
import com.financeapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register and login to receive a JWT token")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new account",
               description = "Self-registered accounts receive the VIEWER role. An admin can elevate roles later.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request), "Registration successful"));
    }

    @Operation(summary = "Login",
               description = "Returns a Bearer JWT token. Pass it as `Authorization: Bearer <token>` on all protected endpoints.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
    }
}
