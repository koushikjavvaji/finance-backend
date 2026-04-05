package com.financeapp.service.impl;

import com.financeapp.dto.request.UserRequest;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.entity.User;
import com.financeapp.exception.BadRequestException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.UserRepository;
import com.financeapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public UserResponse createUser(UserRequest.Create request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateRole(Long id, UserRequest.UpdateRole request) {
        User user = findOrThrow(id);
        user.setRole(request.getRole());
        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateStatus(Long id, UserRequest.UpdateStatus request) {
        User user = findOrThrow(id);
        user.setStatus(request.getStatus());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = findOrThrow(id);
        userRepository.delete(user);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
