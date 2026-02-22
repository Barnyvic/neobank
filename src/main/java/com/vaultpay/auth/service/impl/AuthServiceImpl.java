package com.vaultpay.auth.service.impl;

import com.vaultpay.auth.dto.request.LoginRequest;
import com.vaultpay.auth.dto.request.RegisterRequest;
import com.vaultpay.auth.dto.response.AuthResponse;
import com.vaultpay.auth.mapper.AuthMapper;
import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.auth.service.AuthService;
import com.vaultpay.auth.service.JwtService;
import com.vaultpay.auth.service.LoginAttemptService;
import com.vaultpay.auth.service.RefreshTokenStore;
import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", request.email());
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("User", request.phoneNumber());
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .phoneNumber(request.phoneNumber().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .kycLevel(KycLevel.TIER_1)
                .transactionPin(null)
                .build();
        user = userRepository.save(user);

        UserDetails userDetails = new UserPrincipal(user, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenStore.createToken(user.getId());
        long expiresIn = accessTokenExpirationMs / 1000;

        return AuthMapper.toResponse(accessToken, refreshToken, expiresIn);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        if (loginAttemptService.isLocked(email)) {
            throw new BusinessException(
                    "Account temporarily locked due to too many failed login attempts. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCode.ACCOUNT_LOCKED);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));

            loginAttemptService.recordSuccess(email);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserPrincipal principal = (UserPrincipal) userDetails;
            Long userId = principal.getUser().getId();

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = refreshTokenStore.createToken(userId);
            long expiresIn = accessTokenExpirationMs / 1000;

            return AuthMapper.toResponse(accessToken, refreshToken, expiresIn);
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        Long userId = refreshTokenStore.getUserId(refreshToken);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenStore.revoke(refreshToken);
        String newRefreshToken = refreshTokenStore.createToken(user.getId());

        UserDetails userDetails = new UserPrincipal(user, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String accessToken = jwtService.generateAccessToken(userDetails);
        long expiresIn = accessTokenExpirationMs / 1000;

        return AuthMapper.toResponse(accessToken, newRefreshToken, expiresIn);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }
}
