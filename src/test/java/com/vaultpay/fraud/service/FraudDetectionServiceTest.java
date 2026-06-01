package com.vaultpay.fraud.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.fraud.repository.FraudAlertRepository;
import com.vaultpay.fraud.rule.FraudRule;
import com.vaultpay.fraud.rule.FraudRuleResult;
import com.vaultpay.fraud.service.impl.FraudDetectionServiceImpl;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private UserRepository userRepository;

    private FraudProperties properties;
    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        properties = new FraudProperties();
        properties.setEnabled(true);
    }

    @Test
    @DisplayName("should allow transaction when no rules trigger")
    void shouldAllowWhenNoRulesTrigger() {
        FraudRule noOpRule = context -> Optional.empty();
        fraudDetectionService = new FraudDetectionServiceImpl(
                properties, List.of(noOpRule), fraudAlertRepository, userRepository);

        assertThatCode(() -> fraudDetectionService.evaluateOrThrow(
                        FraudCheckContext.forTransfer(1L, BigDecimal.valueOf(100), "NGN", "WLT-001")))
                .doesNotThrowAnyException();
        verifyNoInteractions(fraudAlertRepository);
    }

    @Test
    @DisplayName("should block transaction when a BLOCK rule triggers")
    void shouldBlockWhenBlockRuleTriggers() {
        FraudRule blockRule = context -> Optional.of(new FraudRuleResult(
                FraudRuleType.SINGLE_TRANSACTION_LIMIT, FraudSeverity.BLOCK, "Blocked"));
        fraudDetectionService = new FraudDetectionServiceImpl(
                properties, List.of(blockRule), fraudAlertRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).email("u@test.com").build()));

        assertThatThrownBy(() -> fraudDetectionService.evaluateOrThrow(
                        FraudCheckContext.forTransfer(1L, BigDecimal.valueOf(100), "NGN", "WLT-001")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assert be.getErrorCode() == ErrorCode.FRAUD_DETECTED;
                });

        verify(fraudAlertRepository).save(any());
    }

    @Test
    @DisplayName("should allow but persist alert when only REVIEW rules trigger")
    void shouldAllowWithReviewAlert() {
        FraudRule reviewRule = context -> Optional.of(new FraudRuleResult(
                FraudRuleType.NEW_RECIPIENT, FraudSeverity.REVIEW, "Review"));
        fraudDetectionService = new FraudDetectionServiceImpl(
                properties, List.of(reviewRule), fraudAlertRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).email("u@test.com").build()));

        assertThatCode(() -> fraudDetectionService.evaluateOrThrow(
                        FraudCheckContext.forTransfer(1L, BigDecimal.valueOf(100), "NGN", "WLT-001")))
                .doesNotThrowAnyException();

        verify(fraudAlertRepository).save(any());
    }

    @Test
    @DisplayName("should skip checks when fraud detection is disabled")
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);
        FraudRule blockRule = context -> Optional.of(new FraudRuleResult(
                FraudRuleType.SINGLE_TRANSACTION_LIMIT, FraudSeverity.BLOCK, "Blocked"));
        fraudDetectionService = new FraudDetectionServiceImpl(
                properties, List.of(blockRule), fraudAlertRepository, userRepository);

        assertThatCode(() -> fraudDetectionService.evaluateOrThrow(
                        FraudCheckContext.forTransfer(1L, BigDecimal.valueOf(100), "NGN", "WLT-001")))
                .doesNotThrowAnyException();

        verifyNoInteractions(fraudAlertRepository, userRepository);
    }
}
