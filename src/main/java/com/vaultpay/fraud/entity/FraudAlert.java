package com.vaultpay.fraud.entity;

import com.vaultpay.common.audit.Auditable;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private FraudRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FraudAlertStatus status = FraudAlertStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private FraudOperationType operationType;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 5)
    private String currency;

    @Column(name = "recipient_wallet_number", length = 50)
    private String recipientWalletNumber;

    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}
