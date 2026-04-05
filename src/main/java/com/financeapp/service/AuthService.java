package com.financeapp.service;

import com.financeapp.dto.request.AuthRequest;
import com.financeapp.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(AuthRequest.Register request);
    AuthResponse login(AuthRequest.Login request);
}
