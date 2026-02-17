package com.vaultpay.wallet.repository;

import com.vaultpay.wallet.entity.Wallet;
import com.vaultpay.wallet.enums.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByWalletNumber(String walletNumber);

    List<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Currency currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(Long id);

    boolean existsByUserIdAndCurrency(Long userId, Currency currency);
}
