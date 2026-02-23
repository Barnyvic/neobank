package com.vaultpay.paystack.service;

import com.vaultpay.paystack.dto.InitializePaymentResponse;
import com.vaultpay.paystack.dto.InitiateTransferResponse;
import com.vaultpay.paystack.dto.TransferRecipientResponse;
import com.vaultpay.paystack.dto.VerifyPaymentResponse;

import java.math.BigDecimal;

public interface PaystackService {

    InitializePaymentResponse initializePayment(
            String email, BigDecimal amount, String reference, String currency);

    VerifyPaymentResponse verifyPayment(String reference);

    boolean verifyWebhookSignature(String payload, String signature);

    TransferRecipientResponse createTransferRecipient(
            String bankCode, String accountNumber, String name);

    InitiateTransferResponse initiateTransfer(
            String recipientCode, BigDecimal amount, String reference, String reason, String currency);
}
