package com.vaultpay.paystack.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.paystack.config.PaystackConfig;
import com.vaultpay.paystack.dto.*;
import com.vaultpay.paystack.service.PaystackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackServiceImpl implements PaystackService {

    private final PaystackConfig paystackConfig;
    private final RestTemplate restTemplate;

    @Override
    public InitializePaymentResponse initializePayment(
            String email, BigDecimal amount, String reference, String currency) {
        String url = paystackConfig.getBaseUrl() + "/transaction/initialize";

        InitializePaymentRequest request = InitializePaymentRequest.builder()
                .email(email)
                .amount(amount.toBigInteger().toString())
                .reference(reference)
                .currency(currency)
                .build();

        HttpHeaders headers = buildHeaders();
        HttpEntity<InitializePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<InitializePaymentResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, InitializePaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Paystack initialize payment failed: {}", e.getMessage());
            throw new BusinessException("Payment initialization failed. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public VerifyPaymentResponse verifyPayment(String reference) {
        String url = paystackConfig.getBaseUrl() + "/transaction/verify/" + reference;

        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<VerifyPaymentResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, VerifyPaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Paystack verify payment failed: {}", e.getMessage());
            throw new BusinessException("Payment verification failed. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    paystackConfig.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TransferRecipientResponse createTransferRecipient(
            String bankCode, String accountNumber, String name) {
        String url = paystackConfig.getBaseUrl() + "/transferrecipient";

        TransferRecipientRequest request = TransferRecipientRequest.builder()
                .type("nuban")
                .name(name)
                .accountNumber(accountNumber)
                .bankCode(bankCode)
                .currency("NGN")
                .build();

        HttpHeaders headers = buildHeaders();
        HttpEntity<TransferRecipientRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TransferRecipientResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, TransferRecipientResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Paystack create transfer recipient failed: {}", e.getMessage());
            throw new BusinessException("Failed to create transfer recipient.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public InitiateTransferResponse initiateTransfer(
            String recipientCode, BigDecimal amount, String reference, String reason, String currency) {
        String url = paystackConfig.getBaseUrl() + "/transfer";

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .source("balance")
                .reason(reason)
                .amount(amount.toBigInteger().toString())
                .recipient(recipientCode)
                .reference(reference)
                .currency(currency)
                .build();

        HttpHeaders headers = buildHeaders();
        HttpEntity<InitiateTransferRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<InitiateTransferResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, InitiateTransferResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Paystack initiate transfer failed: ref={}, error={}", reference, e.getMessage());
            throw new BusinessException("Transfer initiation failed.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paystackConfig.getSecretKey());
        return headers;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
