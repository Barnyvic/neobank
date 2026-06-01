package com.vaultpay.fraud.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.fraud.dto.request.ResolveFraudAlertRequest;
import com.vaultpay.fraud.dto.response.FraudAlertResponse;
import com.vaultpay.fraud.entity.FraudAlert;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import com.vaultpay.fraud.repository.FraudAlertRepository;
import com.vaultpay.fraud.service.FraudAlertService;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraudAlertServiceImpl implements FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> listAlerts(FraudAlertStatus status, Pageable pageable) {
        Page<FraudAlert> page = status != null
                ? fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(FraudAlertResponse::from);
    }

    @Override
    @Transactional
    public FraudAlertResponse resolveAlert(Long alertId, Long adminUserId, ResolveFraudAlertRequest request) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud alert", alertId.toString()));

        if (alert.getStatus() != FraudAlertStatus.OPEN) {
            throw new BusinessException("Alert is already closed", HttpStatus.CONFLICT, ErrorCode.CONFLICT);
        }

        if (request.status() == FraudAlertStatus.OPEN) {
            throw new BusinessException(
                    "Resolution status must be RESOLVED or DISMISSED", HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId.toString()));

        alert.setStatus(request.status());
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(admin);
        alert.setResolutionNote(request.note());

        return FraudAlertResponse.from(fraudAlertRepository.save(alert));
    }
}
