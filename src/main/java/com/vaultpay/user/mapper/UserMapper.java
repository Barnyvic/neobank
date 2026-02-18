package com.vaultpay.user.mapper;

import com.vaultpay.user.dto.response.UserResponse;
import com.vaultpay.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .kycLevel(user.getKycLevel() != null ? user.getKycLevel().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
