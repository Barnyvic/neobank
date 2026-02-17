package com.vaultpay.paystack.service;

import com.vaultpay.paystack.dto.InitializePaymentResponse;
import com.vaultpay.paystack.dto.VerifyPaymentResponse;

import java.math.BigDecimal;

public interface PaystackService {

    InitializePaymentResponse initializePayment(
            String email, BigDecimal amount, String reference, String currency);

    VerifyPaymentResponse verifyPayment(String reference);

    boolean verifyWebhookSignature(String payload, String signature);
}
