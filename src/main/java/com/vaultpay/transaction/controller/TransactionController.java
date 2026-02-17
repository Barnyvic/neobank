package com.vaultpay.transaction.controller;

import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.transaction.dto.request.FundWalletRequest;
import com.vaultpay.transaction.dto.request.TransferRequest;
import com.vaultpay.transaction.dto.request.WithdrawRequest;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Money transfers, deposits, and withdrawals")
public class TransactionController {

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money to another wallet")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        // TODO: Get authenticated user, delegate to TransactionService
        return ResponseEntity.ok(ApiResponse.success("Transfer initiated", null));
    }

    @PostMapping("/fund")
    @Operation(summary = "Fund wallet via Paystack")
    public ResponseEntity<ApiResponse<TransactionResponse>> fundWallet(@Valid @RequestBody FundWalletRequest request) {
        // TODO: Delegate to TransactionService
        return ResponseEntity.ok(ApiResponse.success("Funding initiated", null));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw to bank account")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody WithdrawRequest request) {
        // TODO: Delegate to TransactionService
        return ResponseEntity.ok(ApiResponse.success("Withdrawal initiated", null));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction by reference")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable String reference) {
        // TODO: Delegate to TransactionService
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get transaction history for a wallet")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @PathVariable Long walletId, Pageable pageable) {
        // TODO: Delegate to TransactionService
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
