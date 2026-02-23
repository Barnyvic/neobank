package com.vaultpay.ledger.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.entity.LedgerEntry;
import com.vaultpay.ledger.enums.AccountType;
import com.vaultpay.ledger.enums.EntryType;
import com.vaultpay.ledger.repository.JournalEntryRepository;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;

    @Override
    @Transactional
    public JournalEntry postJournalEntry(JournalEntry journalEntry) {
        validateBalanced(journalEntry);

        JournalEntry saved = journalEntryRepository.save(journalEntry);

        for (LedgerEntry entry : saved.getEntries()) {
            LedgerAccount account = ledgerAccountRepository.findByIdWithLock(entry.getLedgerAccount().getId())
                    .orElseThrow(() -> new BusinessException(
                            "Ledger account not found: " + entry.getLedgerAccount().getId(),
                            HttpStatus.INTERNAL_SERVER_ERROR));

            BigDecimal adjustment = computeAdjustment(account.getAccountType(), entry.getEntryType(), entry.getAmount());
            account.setBalance(account.getBalance().add(adjustment));
            ledgerAccountRepository.save(account);
        }

        log.debug("Posted journal entry ref={}, entries={}", saved.getReference(), saved.getEntries().size());
        return saved;
    }

    @Override
    @Transactional
    public JournalEntry createTransferEntry(
            LedgerAccount sourceAccount, LedgerAccount destAccount,
            BigDecimal amount, String reference, String description) {

        JournalEntry journal = JournalEntry.builder()
                .reference(reference)
                .description(description)
                .build();

        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(sourceAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .build());

        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(destAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .build());

        return postJournalEntry(journal);
    }

    @Override
    @Transactional
    public JournalEntry createFundingEntry(
            LedgerAccount userAccount, LedgerAccount paystackLiabilityAccount,
            BigDecimal amount, String reference, String description) {

        JournalEntry journal = JournalEntry.builder()
                .reference(reference)
                .description(description)
                .build();

        
        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(paystackLiabilityAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .build());

        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(userAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .build());

        return postJournalEntry(journal);
    }

    @Override
    @Transactional
    public JournalEntry createWithdrawalEntry(
            LedgerAccount userAccount, LedgerAccount paystackLiabilityAccount,
            BigDecimal amount, String reference, String description) {

        JournalEntry journal = JournalEntry.builder()
                .reference(reference)
                .description(description)
                .build();

        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(userAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .build());

        journal.addEntry(LedgerEntry.builder()
                .ledgerAccount(paystackLiabilityAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .build());

        return postJournalEntry(journal);
    }

    @Override
    @Transactional
    public LedgerAccount getOrCreateSystemAccount(String accountName) {
        return ledgerAccountRepository
                .findByAccountNameAndAccountType(accountName, AccountType.LIABILITY)
                .orElseGet(() -> {
                    LedgerAccount account = LedgerAccount.builder()
                            .accountName(accountName)
                            .accountType(AccountType.LIABILITY)
                            .build();
                    return ledgerAccountRepository.save(account);
                });
    }

    private void validateBalanced(JournalEntry journalEntry) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : journalEntry.getEntries()) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException(
                    "Unbalanced journal entry: debits=" + totalDebits + " credits=" + totalCredits,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private BigDecimal computeAdjustment(AccountType accountType, EntryType entryType, BigDecimal amount) {
        boolean naturalDebit = accountType == AccountType.ASSET || accountType == AccountType.EXPENSE;
        if (naturalDebit) {
            return entryType == EntryType.DEBIT ? amount : amount.negate();
        } else {
            return entryType == EntryType.CREDIT ? amount : amount.negate();
        }
    }
}
