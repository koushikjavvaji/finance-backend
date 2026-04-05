package com.financeapp.dto.request;

import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class UserRequest {

    @Data
    public static class Create {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotNull(message = "Role is required")
        private Role role;
    }

    @Data
    public static class UpdateRole {
        @NotNull(message = "Role is required")
        private Role role;
    }

    @Data
    public static class UpdateStatus {
        @NotNull(message = "Status is required")
        private UserStatus status;
    }
}
