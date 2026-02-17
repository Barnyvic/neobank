package com.vaultpay.auth.service;

import com.vaultpay.auth.dto.request.LoginRequest;
import com.vaultpay.auth.dto.request.RegisterRequest;
import com.vaultpay.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);
}
