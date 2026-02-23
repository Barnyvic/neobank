package com.vaultpay.paystack.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultpay.paystack.service.PaystackService;
import com.vaultpay.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/paystack")
@Tag(name = "Paystack", description = "Paystack webhook and payment callbacks")
@RequiredArgsConstructor
public class PaystackWebhookController {

    private final PaystackService paystackService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    @Operation(summary = "Handle Paystack webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {

        if (!paystackService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();

            if ("charge.success".equals(event)) {
                JsonNode data = root.path("data");
                String reference = data.path("reference").asText();
                String paystackRef = data.path("id").asText();

                log.info("Processing Paystack charge.success: ref={}", reference);
                transactionService.completeFunding(reference, paystackRef);
            }
        } catch (Exception e) {
            log.error("Error processing Paystack webhook", e);
        }

        return ResponseEntity.ok("OK");
    }
}
