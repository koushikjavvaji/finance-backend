package com.financeapp.controller;

import com.financeapp.dto.request.UserRequest;
import com.financeapp.dto.response.ApiResponse;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User management — ADMIN only")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @Operation(summary = "Create a user with an explicit role")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserRequest.Create request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.createUser(request), "User created successfully"));
    }

    @Operation(summary = "Update a user's role", description = "Body: { \"role\": \"ANALYST\" }")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest.UpdateRole request) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.updateRole(id, request), "Role updated successfully"));
    }

    @Operation(summary = "Activate or deactivate a user", description = "Body: { \"status\": \"INACTIVE\" }")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest.UpdateStatus request) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.updateStatus(id, request), "Status updated successfully"));
    }

    @Operation(summary = "Delete a user (hard delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
