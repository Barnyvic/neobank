package com.vaultpay.fraud.service;

import com.vaultpay.fraud.dto.request.ResolveFraudAlertRequest;
import com.vaultpay.fraud.dto.response.FraudAlertResponse;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FraudAlertService {

    Page<FraudAlertResponse> listAlerts(FraudAlertStatus status, Pageable pageable);

    FraudAlertResponse resolveAlert(Long alertId, Long adminUserId, ResolveFraudAlertRequest request);
}
