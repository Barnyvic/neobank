package com.vaultpay.transaction.entity;

import com.vaultpay.common.audit.Auditable;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "NGN";

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id")
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dest_wallet_id")
    private Wallet destWallet;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(columnDefinition = "jsonb")
    private String metadata;
}
