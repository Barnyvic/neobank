package com.vaultpay.transaction.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.enums.AccountType;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.service.LedgerService;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.impl.RefundServiceImpl;
import com.vaultpay.user.entity.User;
import com.vaultpay.wallet.entity.Wallet;
import com.vaultpay.wallet.enums.Currency;
import com.vaultpay.wallet.enums.WalletStatus;
import com.vaultpay.wallet.repository.WalletRepository;
import com.vaultpay.wallet.service.WalletCacheEvictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Tests")
class RefundServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private WalletCacheEvictionService walletCacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RefundServiceImpl refundService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("reverseTransaction")
    class ReverseTransaction {

        @Test
        @DisplayName("should reverse an EXTERNAL_FAILED withdrawal and credit wallet")
        void shouldReverseExternalFailedWithdrawal() {
            User user = User.builder().id(USER_ID).build();
            Wallet wallet = Wallet.builder()
                    .id(1L).walletNumber("1111111111").currency(Currency.NGN)
                    .status(WalletStatus.ACTIVE).user(user).balance(BigDecimal.valueOf(0))
                    .build();
            Transaction original = Transaction.builder()
                    .id(100L).reference("WTH-ABC123").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.EXTERNAL_FAILED).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").sourceWallet(wallet).build();

            LedgerAccount userLedger = LedgerAccount.builder()
                    .id(10L).accountName("WALLET:1111111111").accountType(AccountType.ASSET).wallet(wallet).build();
            LedgerAccount systemLedger = LedgerAccount.builder()
                    .id(20L).accountName("PAYSTACK_LIABILITY").accountType(AccountType.LIABILITY).build();

            when(transactionRepository.findByReference("WTH-ABC123")).thenReturn(Optional.of(original));
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(userLedger));
            when(ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY")).thenReturn(systemLedger);
            when(ledgerService.createReversalEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("REV-123").description("Reversal").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse response = refundService.reverseTransaction("WTH-ABC123");

            assertThat(response.transactionType()).isEqualTo("REVERSAL");
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(original.getStatus()).isEqualTo(TransactionStatus.REVERSED);
            verify(ledgerService).createReversalEntry(eq(userLedger), eq(systemLedger), any(), anyString(), anyString());
            verify(walletCacheEvictionService).evictWalletCaches(1L, USER_ID);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should return existing result if already REVERSED")
        void shouldReturnIfAlreadyReversed() {
            Transaction original = Transaction.builder()
                    .id(100L).reference("WTH-ABC123").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.REVERSED).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("WTH-ABC123")).thenReturn(Optional.of(original));

            TransactionResponse response = refundService.reverseTransaction("WTH-ABC123");

            assertThat(response.status()).isEqualTo("REVERSED");
            verify(walletRepository, never()).findByIdWithLock(any());
        }

        @Test
        @DisplayName("should throw when transaction status is not reversible")
        void shouldThrowWhenNotReversible() {
            Transaction original = Transaction.builder()
                    .id(100L).reference("WTH-ABC123").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("WTH-ABC123")).thenReturn(Optional.of(original));

            assertThatThrownBy(() -> refundService.reverseTransaction("WTH-ABC123"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.REVERSAL_FAILED);
                    });
        }

        @Test
        @DisplayName("should throw when transaction not found")
        void shouldThrowWhenNotFound() {
            when(transactionRepository.findByReference("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refundService.reverseTransaction("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should reverse a PENDING_EXTERNAL transaction")
        void shouldReversePendingExternal() {
            User user = User.builder().id(USER_ID).build();
            Wallet wallet = Wallet.builder()
                    .id(1L).walletNumber("1111111111").currency(Currency.NGN)
                    .status(WalletStatus.ACTIVE).user(user).balance(BigDecimal.valueOf(1000))
                    .build();
            Transaction original = Transaction.builder()
                    .id(200L).reference("WTH-XYZ").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.PENDING_EXTERNAL).amount(BigDecimal.valueOf(3000))
                    .currency("NGN").sourceWallet(wallet).build();

            LedgerAccount userLedger = LedgerAccount.builder()
                    .id(10L).accountName("WALLET:1111111111").accountType(AccountType.ASSET).wallet(wallet).build();
            LedgerAccount systemLedger = LedgerAccount.builder()
                    .id(20L).accountName("PAYSTACK_LIABILITY").accountType(AccountType.LIABILITY).build();

            when(transactionRepository.findByReference("WTH-XYZ")).thenReturn(Optional.of(original));
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(userLedger));
            when(ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY")).thenReturn(systemLedger);
            when(ledgerService.createReversalEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("REV-456").description("Reversal").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse response = refundService.reverseTransaction("WTH-XYZ");

            assertThat(response.transactionType()).isEqualTo("REVERSAL");
            assertThat(original.getStatus()).isEqualTo(TransactionStatus.REVERSED);
            verify(ledgerService).createReversalEntry(eq(userLedger), eq(systemLedger), any(), anyString(), anyString());
        }
    }
}
