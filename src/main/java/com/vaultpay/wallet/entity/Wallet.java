package com.vaultpay.wallet.entity;

import com.vaultpay.common.audit.Auditable;
import com.vaultpay.user.entity.User;
import com.vaultpay.wallet.enums.Currency;
import com.vaultpay.wallet.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_number", nullable = false, unique = true)
    private String walletNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Currency currency = Currency.NGN;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;
}
