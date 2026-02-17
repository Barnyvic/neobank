package com.vaultpay.user.controller;

import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile management")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        // TODO: Get authenticated user from SecurityContext, delegate to UserService
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        // TODO: Delegate to UserService
        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
    }

    @PostMapping("/me/pin")
    @Operation(summary = "Set or update transaction PIN")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(@Valid @RequestBody SetTransactionPinRequest request) {
        // TODO: Delegate to UserService
        return ResponseEntity.ok(ApiResponse.success("Transaction PIN set successfully", null));
    }
}
