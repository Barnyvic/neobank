package com.vaultpay.user.service;

import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.response.UserResponse;

public interface UserService {

    UserResponse getUserById(Long userId);

    UserResponse getUserByEmail(String email);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void setTransactionPin(Long userId, SetTransactionPinRequest request);

    boolean verifyTransactionPin(Long userId, String pin);
}
