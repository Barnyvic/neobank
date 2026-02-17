package com.vaultpay.ledger.repository;

import com.vaultpay.ledger.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByReference(String reference);

    boolean existsByReference(String reference);
}
