package com.vaultpay.paystack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paystack")
@Tag(name = "Paystack", description = "Paystack webhook and payment callbacks")
public class PaystackWebhookController {

    @PostMapping("/webhook")
    @Operation(summary = "Handle Paystack webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {
        // TODO: Verify signature, parse event, delegate to TransactionService
        return ResponseEntity.ok("OK");
    }
}
