package com.vaultpay.ledger.entity;

import com.vaultpay.common.audit.Auditable;
import com.vaultpay.ledger.enums.AccountType;
import com.vaultpay.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ledger_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
