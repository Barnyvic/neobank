package com.vaultpay.ledger.service;

import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;

import java.math.BigDecimal;

public interface LedgerService {

    JournalEntry postJournalEntry(JournalEntry journalEntry);

    JournalEntry createTransferEntry(
            LedgerAccount sourceAccount,
            LedgerAccount destAccount,
            BigDecimal amount,
            String reference,
            String description
    );

    JournalEntry createFundingEntry(
            LedgerAccount userAccount,
            LedgerAccount paystackLiabilityAccount,
            BigDecimal amount,
            String reference,
            String description
    );

    JournalEntry createWithdrawalEntry(
            LedgerAccount userAccount,
            LedgerAccount paystackLiabilityAccount,
            BigDecimal amount,
            String reference,
            String description
    );

    LedgerAccount getOrCreateSystemAccount(String accountName);
}
