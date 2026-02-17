package com.vaultpay.transaction.service;

import com.vaultpay.transaction.dto.request.FundWalletRequest;
import com.vaultpay.transaction.dto.request.TransferRequest;
import com.vaultpay.transaction.dto.request.WithdrawRequest;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse transfer(Long userId, TransferRequest request);

    TransactionResponse initiateFunding(Long userId, FundWalletRequest request);

    TransactionResponse completeFunding(String reference, String paystackReference);

    TransactionResponse withdraw(Long userId, WithdrawRequest request);

    TransactionResponse getTransaction(String reference);

    Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable);
}
