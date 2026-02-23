package com.vaultpay.transaction.controller;

import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.transaction.dto.request.FundWalletRequest;
import com.vaultpay.transaction.dto.request.TransferRequest;
import com.vaultpay.transaction.dto.request.WithdrawRequest;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import com.vaultpay.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Money transfers, deposits, and withdrawals")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money to another wallet")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.transfer(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed", response));
    }

    @PostMapping("/fund")
    @Operation(summary = "Fund wallet via Paystack")
    public ResponseEntity<ApiResponse<TransactionResponse>> fundWallet(@Valid @RequestBody FundWalletRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.initiateFunding(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Funding initiated", response));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw to bank account")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.withdraw(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal completed", response));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction by reference")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable String reference) {
        TransactionResponse response = transactionService.getTransaction(reference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get transaction history for a wallet")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @PathVariable Long walletId, Pageable pageable) {
        Page<TransactionResponse> history = transactionService.getTransactionHistory(walletId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return ((UserPrincipal) auth.getPrincipal()).getUser().getId();
    }
}
