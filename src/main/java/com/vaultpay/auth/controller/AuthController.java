package com.vaultpay.auth.controller;

import com.vaultpay.auth.dto.request.LoginRequest;
import com.vaultpay.auth.dto.request.RefreshTokenRequest;
import com.vaultpay.auth.dto.request.RegisterRequest;
import com.vaultpay.auth.dto.response.AuthResponse;
import com.vaultpay.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // TODO: Delegate to AuthService
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User registered successfully", null));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // TODO: Delegate to AuthService
        return ResponseEntity.ok(ApiResponse.success("Login successful", null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        // TODO: Delegate to AuthService
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // TODO: Delegate to AuthService
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
