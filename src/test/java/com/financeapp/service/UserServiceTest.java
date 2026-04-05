package com.financeapp.service;

import com.financeapp.dto.request.UserRequest;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import com.financeapp.exception.BadRequestException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.UserRepository;
import com.financeapp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserServiceImpl userService;

    private User adminUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).name("Admin").email("admin@finance.com")
                .password("hashed").role(Role.ADMIN).status(UserStatus.ACTIVE)
                .build();

        viewerUser = User.builder()
                .id(2L).name("Viewer").email("viewer@finance.com")
                .password("hashed").role(Role.VIEWER).status(UserStatus.ACTIVE)
                .build();
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsAllUsersAsDtos() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, viewerUser));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponse::getEmail)
                .containsExactly("admin@finance.com", "viewer@finance.com");
    }

    @Test
    void getAllUsers_whenEmpty_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertThat(userService.getAllUsers()).isEmpty();
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    void getUserById_withValidId_returnsUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        UserResponse result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void getUserById_withInvalidId_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    void createUser_withValidData_savesAndReturnsDto() {
        UserRequest.Create req = new UserRequest.Create();
        req.setName("New Analyst");
        req.setEmail("analyst@finance.com");
        req.setPassword("pass123");
        req.setRole(Role.ANALYST);

        User saved = User.builder().id(3L).name("New Analyst")
                .email("analyst@finance.com").password("hashed")
                .role(Role.ANALYST).status(UserStatus.ACTIVE).build();

        when(userRepository.existsByEmail("analyst@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(saved);

        UserResponse result = userService.createUser(req);

        assertThat(result.getEmail()).isEqualTo("analyst@finance.com");
        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    void createUser_withDuplicateEmail_throwsBadRequestException() {
        UserRequest.Create req = new UserRequest.Create();
        req.setEmail("admin@finance.com");

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
    }

    // ── updateRole ────────────────────────────────────────────────────────────

    @Test
    void updateRole_updatesAndReturnsUpdatedDto() {
        UserRequest.UpdateRole req = new UserRequest.UpdateRole();
        req.setRole(Role.ANALYST);

        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(viewerUser)).thenReturn(viewerUser);

        UserResponse result = userService.updateRole(2L, req);

        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
        verify(userRepository).save(viewerUser);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_toInactive_deactivatesUser() {
        UserRequest.UpdateStatus req = new UserRequest.UpdateStatus();
        req.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(viewerUser)).thenReturn(viewerUser);

        UserResponse result = userService.updateStatus(2L, req);

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_withValidId_callsRepositoryDelete() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));

        userService.deleteUser(2L);

        verify(userRepository).delete(viewerUser);
    }

    @Test
    void deleteUser_withInvalidId_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
