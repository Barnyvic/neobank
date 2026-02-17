package com.vaultpay.ledger.repository;

import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByWalletId(Long walletId);

    Optional<LedgerAccount> findByAccountNameAndAccountType(String accountName, AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT la FROM LedgerAccount la WHERE la.id = :id")
    Optional<LedgerAccount> findByIdWithLock(Long id);
}
