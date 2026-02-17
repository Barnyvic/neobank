package com.vaultpay.transaction.repository;

import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(
            Long sourceWalletId, Long destWalletId, Pageable pageable);

    Page<Transaction> findBySourceWalletIdAndStatusOrderByCreatedAtDesc(
            Long walletId, TransactionStatus status, Pageable pageable);
}
