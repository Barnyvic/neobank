package com.vaultpay.wallet.controller;

import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.wallet.dto.request.CreateWalletRequest;
import com.vaultpay.wallet.dto.response.BalanceResponse;
import com.vaultpay.wallet.dto.response.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet creation and management")
public class WalletController {

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        // TODO: Get authenticated user, delegate to WalletService
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Wallet created", null));
    }

    @GetMapping
    @Operation(summary = "Get all wallets for current user")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets() {
        // TODO: Delegate to WalletService
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet by ID")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Long walletId) {
        // TODO: Delegate to WalletService
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long walletId) {
        // TODO: Delegate to WalletService
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
