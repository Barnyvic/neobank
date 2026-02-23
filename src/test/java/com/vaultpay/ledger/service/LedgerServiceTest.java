package com.vaultpay.ledger.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.entity.LedgerEntry;
import com.vaultpay.ledger.enums.AccountType;
import com.vaultpay.ledger.enums.EntryType;
import com.vaultpay.ledger.repository.JournalEntryRepository;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.service.impl.LedgerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerService Tests")
class LedgerServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Nested
    @DisplayName("postJournalEntry")
    class PostJournalEntry {

        @Test
        @DisplayName("should reject unbalanced journal entry")
        void shouldRejectUnbalancedEntry() {
            LedgerAccount account = buildAccount(1L, AccountType.ASSET);

            JournalEntry journal = JournalEntry.builder()
                    .reference("TEST-001")
                    .description("Unbalanced")
                    .build();
            journal.addEntry(LedgerEntry.builder()
                    .ledgerAccount(account)
                    .entryType(EntryType.DEBIT)
                    .amount(BigDecimal.valueOf(1000))
                    .build());

            assertThatThrownBy(() -> ledgerService.postJournalEntry(journal))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unbalanced journal entry");
        }

        @Test
        @DisplayName("should post balanced journal entry and update ledger accounts")
        void shouldPostBalancedEntry() {
            LedgerAccount sourceAccount = buildAccount(1L, AccountType.ASSET);
            sourceAccount.setBalance(BigDecimal.valueOf(5000));
            LedgerAccount destAccount = buildAccount(2L, AccountType.ASSET);
            destAccount.setBalance(BigDecimal.ZERO);

            JournalEntry journal = JournalEntry.builder()
                    .reference("TRF-001")
                    .description("Transfer")
                    .build();
            journal.addEntry(LedgerEntry.builder()
                    .ledgerAccount(sourceAccount)
                    .entryType(EntryType.DEBIT)
                    .amount(BigDecimal.valueOf(1000))
                    .build());
            journal.addEntry(LedgerEntry.builder()
                    .ledgerAccount(destAccount)
                    .entryType(EntryType.CREDIT)
                    .amount(BigDecimal.valueOf(1000))
                    .build());

            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(journal);
            when(ledgerAccountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
            when(ledgerAccountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destAccount));
            when(ledgerAccountRepository.save(any(LedgerAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            JournalEntry result = ledgerService.postJournalEntry(journal);

            assertThat(result.getReference()).isEqualTo("TRF-001");
            assertThat(sourceAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(6000));
            assertThat(destAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(-1000));
            verify(ledgerAccountRepository, times(2)).save(any(LedgerAccount.class));
        }
    }

    @Nested
    @DisplayName("createTransferEntry")
    class CreateTransferEntry {

        @Test
        @DisplayName("should create a balanced transfer journal entry")
        void shouldCreateTransferEntry() {
            LedgerAccount source = buildAccount(1L, AccountType.ASSET);
            source.setBalance(BigDecimal.valueOf(5000));
            LedgerAccount dest = buildAccount(2L, AccountType.ASSET);
            dest.setBalance(BigDecimal.ZERO);

            when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerAccountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(source));
            when(ledgerAccountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(dest));
            when(ledgerAccountRepository.save(any(LedgerAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            JournalEntry result = ledgerService.createTransferEntry(
                    source, dest, BigDecimal.valueOf(500), "TRF-002", "Test transfer");

            assertThat(result.getEntries()).hasSize(2);
            assertThat(result.getReference()).isEqualTo("TRF-002");
        }
    }

    @Nested
    @DisplayName("getOrCreateSystemAccount")
    class GetOrCreateSystemAccount {

        @Test
        @DisplayName("should return existing system account")
        void shouldReturnExistingAccount() {
            LedgerAccount existing = buildAccount(10L, AccountType.LIABILITY);
            existing.setAccountName("PAYSTACK_LIABILITY");
            when(ledgerAccountRepository.findByAccountNameAndAccountType("PAYSTACK_LIABILITY", AccountType.LIABILITY))
                    .thenReturn(Optional.of(existing));

            LedgerAccount result = ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY");

            assertThat(result.getId()).isEqualTo(10L);
            verify(ledgerAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create system account if not found")
        void shouldCreateIfNotFound() {
            when(ledgerAccountRepository.findByAccountNameAndAccountType("PAYSTACK_LIABILITY", AccountType.LIABILITY))
                    .thenReturn(Optional.empty());
            when(ledgerAccountRepository.save(any(LedgerAccount.class))).thenAnswer(inv -> {
                LedgerAccount saved = inv.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            LedgerAccount result = ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY");

            assertThat(result.getAccountName()).isEqualTo("PAYSTACK_LIABILITY");
            assertThat(result.getAccountType()).isEqualTo(AccountType.LIABILITY);
            verify(ledgerAccountRepository).save(any(LedgerAccount.class));
        }
    }

    private LedgerAccount buildAccount(Long id, AccountType type) {
        return LedgerAccount.builder()
                .id(id)
                .accountName("Account-" + id)
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .build();
    }
}
