package com.vaultpay.fraud.controller;

import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.fraud.dto.request.ResolveFraudAlertRequest;
import com.vaultpay.fraud.dto.response.FraudAlertResponse;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import com.vaultpay.fraud.service.FraudAlertService;
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
@RequestMapping("/api/v1/fraud/alerts")
@Tag(name = "Fraud", description = "Fraud alert review (admin)")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping
    @Operation(summary = "List fraud alerts (admin)")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> listAlerts(
            @RequestParam(required = false) FraudAlertStatus status,
            Pageable pageable) {
        Page<FraudAlertResponse> alerts = fraudAlertService.listAlerts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PatchMapping("/{alertId}")
    @Operation(summary = "Resolve or dismiss a fraud alert (admin)")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolveAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody ResolveFraudAlertRequest request) {
        Long adminUserId = getCurrentUserId();
        FraudAlertResponse response = fraudAlertService.resolveAlert(alertId, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Alert updated", response));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return ((UserPrincipal) auth.getPrincipal()).getUser().getId();
    }
}
