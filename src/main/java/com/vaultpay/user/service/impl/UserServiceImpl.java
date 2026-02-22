package com.vaultpay.user.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.response.UserResponse;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.mapper.UserMapper;
import com.vaultpay.user.repository.UserRepository;
import com.vaultpay.user.service.PinAttemptService;
import com.vaultpay.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PinAttemptService pinAttemptService;

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            String phone = request.phoneNumber().trim();
            userRepository.findByPhoneNumber(phone).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new DuplicateResourceException("User", phone);
                }
            });
            user.setPhoneNumber(phone);
        }

        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Override
    public void setTransactionPin(Long userId, SetTransactionPinRequest request) {
        if (!request.pin().equals(request.confirmPin())) {
            throw new BusinessException("PIN and confirmation do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setTransactionPin(passwordEncoder.encode(request.pin()));
        userRepository.save(user);
    }

    @Override
    public boolean verifyTransactionPin(Long userId, String pin) {
        if (pinAttemptService.isLocked(userId)) {
            throw new BusinessException(
                    "Transaction PIN is temporarily locked due to too many failed attempts. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCode.PIN_LOCKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getTransactionPin() == null || user.getTransactionPin().isBlank()) {
            return false;
        }

        boolean matches = passwordEncoder.matches(pin, user.getTransactionPin());
        if (matches) {
            pinAttemptService.recordSuccess(userId);
        } else {
            pinAttemptService.recordFailure(userId);
        }
        return matches;
    }
}
