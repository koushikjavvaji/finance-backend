package com.financeapp.service;

import com.financeapp.dto.request.UserRequest;
import com.financeapp.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse createUser(UserRequest.Create request);
    UserResponse updateRole(Long id, UserRequest.UpdateRole request);
    UserResponse updateStatus(Long id, UserRequest.UpdateStatus request);
    void deleteUser(Long id);
}
