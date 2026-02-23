package com.vaultpay.wallet.controller;

import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.wallet.dto.request.CreateWalletRequest;
import com.vaultpay.wallet.dto.response.BalanceResponse;
import com.vaultpay.wallet.dto.response.WalletResponse;
import com.vaultpay.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet creation and management")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Long userId = getCurrentUserId();
        WalletResponse response = walletService.createWallet(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Wallet created", response));
    }

    @GetMapping
    @Operation(summary = "Get all wallets for current user")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets() {
        Long userId = getCurrentUserId();
        List<WalletResponse> wallets = walletService.getWalletsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(wallets));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet by ID")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Long walletId) {
        WalletResponse response = walletService.getWalletById(walletId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long walletId) {
        BalanceResponse response = walletService.getBalance(walletId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{walletId}/freeze")
    @Operation(summary = "Freeze a wallet")
    public ResponseEntity<ApiResponse<Void>> freezeWallet(@PathVariable Long walletId) {
        walletService.freezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success("Wallet frozen", null));
    }

    @PostMapping("/{walletId}/unfreeze")
    @Operation(summary = "Unfreeze a wallet")
    public ResponseEntity<ApiResponse<Void>> unfreezeWallet(@PathVariable Long walletId) {
        walletService.unfreezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success("Wallet unfrozen", null));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return ((UserPrincipal) auth.getPrincipal()).getUser().getId();
    }
}
