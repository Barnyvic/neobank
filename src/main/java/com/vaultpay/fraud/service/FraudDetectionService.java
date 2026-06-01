package com.vaultpay.fraud.service;

import com.vaultpay.fraud.dto.FraudCheckContext;

public interface FraudDetectionService {

    void evaluateOrThrow(FraudCheckContext context);
}
