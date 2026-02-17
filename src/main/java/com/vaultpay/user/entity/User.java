package com.vaultpay.user.entity;

import com.vaultpay.common.audit.Auditable;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "transaction_pin")
    private String transactionPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", nullable = false)
    @Builder.Default
    private KycLevel kycLevel = KycLevel.TIER_1;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
