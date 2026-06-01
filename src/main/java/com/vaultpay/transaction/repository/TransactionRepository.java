package com.vaultpay.transaction.repository;

import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(
            Long sourceWalletId, Long destWalletId, Pageable pageable);

    Page<Transaction> findBySourceWalletIdAndStatusOrderByCreatedAtDesc(
            Long walletId, TransactionStatus status, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.sourceWallet.user.id = :userId
            AND t.transactionType IN :types
            AND t.status IN :statuses
            AND t.createdAt >= :since
            """)
    BigDecimal sumOutboundAmountSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("types") Collection<TransactionType> types,
            @Param("statuses") Collection<TransactionStatus> statuses);

    default BigDecimal sumOutboundAmountSince(Long userId, LocalDateTime since) {
        return sumOutboundAmountSince(
                userId,
                since,
                java.util.List.of(TransactionType.TRANSFER, TransactionType.WITHDRAWAL),
                java.util.List.of(
                        TransactionStatus.COMPLETED,
                        TransactionStatus.PROCESSING,
                        TransactionStatus.PENDING_EXTERNAL));
    }

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.sourceWallet.user.id = :userId
            AND t.transactionType IN :types
            AND t.status NOT IN :excludedStatuses
            AND t.createdAt >= :since
            """)
    long countOutboundTransactionsSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("types") Collection<TransactionType> types,
            @Param("excludedStatuses") Collection<TransactionStatus> excludedStatuses);

    default long countOutboundTransactionsSince(Long userId, LocalDateTime since) {
        return countOutboundTransactionsSince(
                userId,
                since,
                java.util.List.of(TransactionType.TRANSFER, TransactionType.WITHDRAWAL),
                java.util.List.of(TransactionStatus.FAILED, TransactionStatus.EXTERNAL_FAILED));
    }

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.sourceWallet.user.id = :userId
            AND t.transactionType = com.vaultpay.transaction.enums.TransactionType.TRANSFER
            AND t.destWallet.walletNumber = :recipientWalletNumber
            AND t.status = com.vaultpay.transaction.enums.TransactionStatus.COMPLETED
            """)
    long countCompletedTransfersToRecipient(
            @Param("userId") Long userId, @Param("recipientWalletNumber") String recipientWalletNumber);
}
