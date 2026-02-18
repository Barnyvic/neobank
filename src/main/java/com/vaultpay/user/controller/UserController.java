package com.vaultpay.user.controller;

import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.response.UserResponse;
import com.vaultpay.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile management")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Long userId = getCurrentUserId();
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        UserResponse userResponse = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userResponse));
    }

    @PostMapping("/me/pin")
    @Operation(summary = "Set or update transaction PIN")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(@Valid @RequestBody SetTransactionPinRequest request) {
        Long userId = getCurrentUserId();
        userService.setTransactionPin(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction PIN set successfully", null));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return ((UserPrincipal) auth.getPrincipal()).getUser().getId();
    }
}
