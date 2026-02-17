package com.vaultpay.ledger.repository;

import com.vaultpay.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByLedgerAccountIdOrderByCreatedAtDesc(Long ledgerAccountId);

    List<LedgerEntry> findByJournalEntryId(Long journalEntryId);
}
