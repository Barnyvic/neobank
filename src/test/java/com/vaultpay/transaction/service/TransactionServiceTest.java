package com.vaultpay.transaction.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.InsufficientFundsException;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.enums.AccountType;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.service.LedgerService;
import com.vaultpay.paystack.dto.InitializePaymentResponse;
import com.vaultpay.paystack.dto.InitiateTransferResponse;
import com.vaultpay.paystack.dto.TransferRecipientResponse;
import com.vaultpay.paystack.service.PaystackService;
import com.vaultpay.transaction.dto.request.FundWalletRequest;
import com.vaultpay.transaction.dto.request.TransferRequest;
import com.vaultpay.transaction.dto.request.WithdrawRequest;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.impl.TransactionServiceImpl;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.service.UserService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private UserService userService;

    @Mock
    private PaystackService paystackService;

    @Mock
    private TransactionThrottleService throttleService;

    @Mock
    private TransactionLockService lockService;

    @Mock
    private WalletCacheEvictionService walletCacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("should complete transfer successfully")
        void shouldCompleteTransfer() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", "idem-1");

            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);
            LedgerAccount srcLedger = buildLedgerAccount(10L, source);
            LedgerAccount destLedger = buildLedgerAccount(20L, dest);

            when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(source));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(srcLedger));
            when(ledgerAccountRepository.findByWalletId(2L)).thenReturn(Optional.of(destLedger));
            when(ledgerService.createTransferEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("TRF-123").description("Transfer").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction txn = inv.getArgument(0);
                txn.setId(100L);
                return txn;
            });

            TransactionResponse response = transactionService.transfer(USER_ID, request);

            assertThat(response.transactionType()).isEqualTo("TRANSFER");
            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(ledgerService).createTransferEntry(eq(srcLedger), eq(destLedger), any(), anyString(), anyString());
            verify(lockService).releaseLock(USER_ID, 1L);
        }

        @Test
        @DisplayName("should return existing transaction for duplicate idempotency key")
        void shouldReturnExistingForDuplicateIdempotencyKey() {
            Transaction existing = Transaction.builder()
                    .id(1L).reference("TRF-EXIST").transactionType(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(1000)).currency("NGN")
                    .build();
            when(transactionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));

            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", "idem-dup");
            TransactionResponse response = transactionService.transfer(USER_ID, request);

            assertThat(response.reference()).isEqualTo("TRF-EXIST");
            verifyNoInteractions(throttleService, lockService);
        }

        @Test
        @DisplayName("should reject invalid PIN")
        void shouldRejectInvalidPin() {
            when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(userService.verifyTransactionPin(USER_ID, "wrong")).thenReturn(false);

            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "wrong", "idem-2");

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid transaction PIN");
        }

        @Test
        @DisplayName("should reject throttled duplicate transaction")
        void shouldRejectThrottledTransaction() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", null);
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(false);

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Duplicate transaction");
        }

        @Test
        @DisplayName("should reject when lock cannot be acquired")
        void shouldRejectWhenLockUnavailable() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", null);
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(false);

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already in progress");
        }

        @Test
        @DisplayName("should reject transfer with insufficient funds")
        void shouldRejectInsufficientFunds() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(10000), "Test", "1234", null);
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(100));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);
            LedgerAccount srcLedger = buildLedgerAccount(10L, source);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(source));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(srcLedger));
            when(ledgerAccountRepository.findByWalletId(2L)).thenReturn(Optional.of(buildLedgerAccount(20L, dest)));

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(InsufficientFundsException.class);
            verify(lockService).releaseLock(USER_ID, 1L);
        }
    }

    @Nested
    @DisplayName("initiateFunding")
    class InitiateFunding {

        @Test
        @DisplayName("should create pending funding transaction and return Paystack auth URL")
        void shouldInitiateFunding() {
            User user = User.builder().id(USER_ID).email("test@example.com").build();
            Wallet wallet = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.ZERO);
            FundWalletRequest request = new FundWalletRequest(1L, BigDecimal.valueOf(5000), "idem-fund-1");

            InitializePaymentResponse.PaymentData paymentData = new InitializePaymentResponse.PaymentData();
            paymentData.setAuthorizationUrl("https://paystack.com/pay/abc123");
            InitializePaymentResponse paystackResponse = new InitializePaymentResponse();
            paystackResponse.setStatus(true);
            paystackResponse.setData(paymentData);

            when(transactionRepository.findByIdempotencyKey("idem-fund-1")).thenReturn(Optional.empty());
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction txn = inv.getArgument(0);
                txn.setId(200L);
                return txn;
            });
            when(paystackService.initializePayment(eq("test@example.com"), any(), anyString(), eq("NGN")))
                    .thenReturn(paystackResponse);

            TransactionResponse response = transactionService.initiateFunding(USER_ID, request);

            assertThat(response.transactionType()).isEqualTo("DEPOSIT");
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.paystackAuthorizationUrl()).isEqualTo("https://paystack.com/pay/abc123");
        }
    }

    @Nested
    @DisplayName("completeFunding")
    class CompleteFunding {

        @Test
        @DisplayName("should complete funding and update wallet balance")
        void shouldCompleteFunding() {
            User user = User.builder().id(USER_ID).build();
            Wallet wallet = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(1000));
            LedgerAccount userAccount = buildLedgerAccount(10L, wallet);
            LedgerAccount paystackAccount = LedgerAccount.builder().id(20L)
                    .accountName("PAYSTACK_LIABILITY").accountType(AccountType.LIABILITY).build();

            Transaction pendingTxn = Transaction.builder()
                    .id(200L).reference("FND-123").transactionType(TransactionType.DEPOSIT)
                    .status(TransactionStatus.PENDING).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").destWallet(wallet).build();

            when(transactionRepository.findByReference("FND-123")).thenReturn(Optional.of(pendingTxn));
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(userAccount));
            when(ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY")).thenReturn(paystackAccount);
            when(ledgerService.createFundingEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("FND-123").description("Funding").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse response = transactionService.completeFunding("FND-123", "psk-ref-1");

            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(ledgerService).createFundingEntry(eq(userAccount), eq(paystackAccount), any(), anyString(), anyString());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should return existing result if already completed")
        void shouldReturnIfAlreadyCompleted() {
            Transaction completedTxn = Transaction.builder()
                    .id(200L).reference("FND-123").transactionType(TransactionType.DEPOSIT)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("FND-123")).thenReturn(Optional.of(completedTxn));

            TransactionResponse response = transactionService.completeFunding("FND-123", "psk-ref");

            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(walletRepository, never()).findByIdWithLock(any());
        }
    }

    @Nested
    @DisplayName("getTransaction")
    class GetTransaction {

        @Test
        @DisplayName("should return transaction by reference")
        void shouldReturnByReference() {
            Transaction txn = Transaction.builder()
                    .id(1L).reference("TRF-001").transactionType(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(500))
                    .currency("NGN").build();
            when(transactionRepository.findByReference("TRF-001")).thenReturn(Optional.of(txn));

            TransactionResponse response = transactionService.getTransaction("TRF-001");

            assertThat(response.reference()).isEqualTo("TRF-001");
        }

        @Test
        @DisplayName("should throw when transaction not found")
        void shouldThrowWhenNotFound() {
            when(transactionRepository.findByReference("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransaction("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ConcurrencyProtection")
    class ConcurrencyProtection {

        @Test
        @DisplayName("should release lock even when an exception occurs mid-transfer")
        void shouldReleaseLockOnException() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", null);
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(source));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(buildLedgerAccount(10L, source)));
            when(ledgerAccountRepository.findByWalletId(2L))
                    .thenThrow(new RuntimeException("Simulated DB failure"));

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(RuntimeException.class);

            verify(lockService).releaseLock(USER_ID, 1L);
        }

        @Test
        @DisplayName("should reject concurrent transfer when lock is held by another request")
        void shouldRejectConcurrentTransferWhenLockHeld() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(500), "Test", "1234", null);
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(false);

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_IN_PROGRESS);
                    });

            verify(lockService, never()).releaseLock(any(), any());
        }

        @Test
        @DisplayName("should allow independent transfers when throttle permits them")
        void shouldAllowTransferAfterThrottleWindowExpires() {
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(10000));
            Wallet dest1 = buildWallet(2L, "8888888888", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);
            Wallet dest2 = buildWallet(3L, "7777777777", WalletStatus.ACTIVE, User.builder().id(3L).build(), BigDecimal.ZERO);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));

            when(walletRepository.findByWalletNumber("8888888888")).thenReturn(Optional.of(dest1));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("8888888888"))).thenReturn(true);
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(source));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(buildLedgerAccount(10L, source)));
            when(ledgerAccountRepository.findByWalletId(2L)).thenReturn(Optional.of(buildLedgerAccount(20L, dest1)));
            when(ledgerService.createTransferEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("TRF-A").description("Transfer").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction txn = inv.getArgument(0);
                txn.setId(100L);
                return txn;
            });

            TransferRequest req1 = new TransferRequest("8888888888", BigDecimal.valueOf(500), "First", "1234", "idem-a");
            when(transactionRepository.findByIdempotencyKey("idem-a")).thenReturn(Optional.empty());
            TransactionResponse resp1 = transactionService.transfer(USER_ID, req1);

            assertThat(resp1.status()).isEqualTo("COMPLETED");
            verify(throttleService).checkAndMark(eq(USER_ID), any(), eq("8888888888"));
            verify(lockService).releaseLock(USER_ID, 1L);
        }

        @Test
        @DisplayName("should reject same amount+dest transfer even with different idempotency keys")
        void shouldRejectSameTransferWithDifferentIdempotencyKeys() {
            TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(1000), "Test", "1234", "idem-x");
            User user = User.builder().id(USER_ID).build();
            Wallet source = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(5000));
            Wallet dest = buildWallet(2L, "9999999999", WalletStatus.ACTIVE, User.builder().id(2L).build(), BigDecimal.ZERO);

            when(transactionRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(source));
            when(walletRepository.findByWalletNumber("9999999999")).thenReturn(Optional.of(dest));
            when(throttleService.checkAndMark(eq(USER_ID), any(), eq("9999999999"))).thenReturn(false);

            assertThatThrownBy(() -> transactionService.transfer(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_TRANSACTION);
                    });

            verify(lockService, never()).acquireLock(any(), any());
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("should debit wallet and call Paystack transfer, setting PROCESSING on success")
        void shouldWithdrawAndCallPaystack() {
            User user = User.builder().id(USER_ID).firstName("John").lastName("Doe").build();
            Wallet wallet = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(10000));
            LedgerAccount userLedger = buildLedgerAccount(10L, wallet);
            LedgerAccount systemLedger = LedgerAccount.builder().id(20L)
                    .accountName("PAYSTACK_LIABILITY").accountType(AccountType.LIABILITY).build();

            WithdrawRequest request = new WithdrawRequest(1L, BigDecimal.valueOf(5000), "058", "0123456789", "1234", "idem-w1");

            when(transactionRepository.findByIdempotencyKey("idem-w1")).thenReturn(Optional.empty());
            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(userLedger));
            when(ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY")).thenReturn(systemLedger);
            when(ledgerService.createWithdrawalEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("WTH-123").description("Withdrawal").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction txn = inv.getArgument(0);
                txn.setId(300L);
                return txn;
            });

            TransferRecipientResponse.RecipientData recipientData = new TransferRecipientResponse.RecipientData();
            recipientData.setRecipientCode("RCP_abc");
            TransferRecipientResponse recipientResponse = new TransferRecipientResponse();
            recipientResponse.setStatus(true);
            recipientResponse.setData(recipientData);
            when(paystackService.createTransferRecipient("058", "0123456789", "John Doe"))
                    .thenReturn(recipientResponse);

            InitiateTransferResponse.TransferData transferData = new InitiateTransferResponse.TransferData();
            transferData.setTransferCode("TRF_abc");
            InitiateTransferResponse transferResponse = new InitiateTransferResponse();
            transferResponse.setStatus(true);
            transferResponse.setData(transferData);
            when(paystackService.initiateTransfer(eq("RCP_abc"), any(), anyString(), anyString(), eq("NGN")))
                    .thenReturn(transferResponse);

            TransactionResponse response = transactionService.withdraw(USER_ID, request);

            assertThat(response.status()).isEqualTo("PROCESSING");
            verify(ledgerService).createWithdrawalEntry(eq(userLedger), eq(systemLedger), any(), anyString(), anyString());
            verify(lockService).releaseLock(USER_ID, 1L);
        }

        @Test
        @DisplayName("should set EXTERNAL_FAILED and publish event when Paystack transfer fails")
        void shouldSetExternalFailedWhenPaystackFails() {
            User user = User.builder().id(USER_ID).firstName("John").lastName("Doe").build();
            Wallet wallet = buildWallet(1L, "1111111111", WalletStatus.ACTIVE, user, BigDecimal.valueOf(10000));
            LedgerAccount userLedger = buildLedgerAccount(10L, wallet);
            LedgerAccount systemLedger = LedgerAccount.builder().id(20L)
                    .accountName("PAYSTACK_LIABILITY").accountType(AccountType.LIABILITY).build();

            WithdrawRequest request = new WithdrawRequest(1L, BigDecimal.valueOf(5000), "058", "0123456789", "1234", null);

            when(userService.verifyTransactionPin(USER_ID, "1234")).thenReturn(true);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
            when(lockService.acquireLock(USER_ID, 1L)).thenReturn(true);
            when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(userLedger));
            when(ledgerService.getOrCreateSystemAccount("PAYSTACK_LIABILITY")).thenReturn(systemLedger);
            when(ledgerService.createWithdrawalEntry(any(), any(), any(), anyString(), anyString()))
                    .thenReturn(JournalEntry.builder().reference("WTH-456").description("Withdrawal").build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction txn = inv.getArgument(0);
                txn.setId(301L);
                return txn;
            });
            when(paystackService.createTransferRecipient(any(), any(), any()))
                    .thenThrow(new RuntimeException("Paystack unavailable"));

            TransactionResponse response = transactionService.withdraw(USER_ID, request);

            assertThat(response.status()).isEqualTo("EXTERNAL_FAILED");
            verify(eventPublisher).publishEvent(any(com.vaultpay.common.event.WithdrawalFailedEvent.class));
            verify(lockService).releaseLock(USER_ID, 1L);
        }
    }

    @Nested
    @DisplayName("completeWithdrawal")
    class CompleteWithdrawal {

        @Test
        @DisplayName("should mark PROCESSING withdrawal as COMPLETED")
        void shouldCompleteWithdrawal() {
            Transaction txn = Transaction.builder()
                    .id(300L).reference("WTH-DONE").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.PROCESSING).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("WTH-DONE")).thenReturn(Optional.of(txn));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse response = transactionService.completeWithdrawal("WTH-DONE");

            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should return existing if already COMPLETED")
        void shouldReturnIfAlreadyCompleted() {
            Transaction txn = Transaction.builder()
                    .id(300L).reference("WTH-DONE").transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("WTH-DONE")).thenReturn(Optional.of(txn));

            TransactionResponse response = transactionService.completeWithdrawal("WTH-DONE");

            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("failFunding")
    class FailFunding {

        @Test
        @DisplayName("should mark PENDING funding as FAILED")
        void shouldMarkPendingAsFailed() {
            Transaction txn = Transaction.builder()
                    .id(400L).reference("FND-FAIL").transactionType(TransactionType.DEPOSIT)
                    .status(TransactionStatus.PENDING).amount(BigDecimal.valueOf(10000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("FND-FAIL")).thenReturn(Optional.of(txn));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failFunding("FND-FAIL");

            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
        }

        @Test
        @DisplayName("should ignore charge.failed for non-PENDING transaction")
        void shouldIgnoreForNonPending() {
            Transaction txn = Transaction.builder()
                    .id(400L).reference("FND-FAIL").transactionType(TransactionType.DEPOSIT)
                    .status(TransactionStatus.COMPLETED).amount(BigDecimal.valueOf(10000))
                    .currency("NGN").build();

            when(transactionRepository.findByReference("FND-FAIL")).thenReturn(Optional.of(txn));

            transactionService.failFunding("FND-FAIL");

            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(transactionRepository, never()).save(any());
        }
    }

    private Wallet buildWallet(Long id, String number, WalletStatus status, User user, BigDecimal balance) {
        return Wallet.builder()
                .id(id).walletNumber(number).currency(Currency.NGN)
                .status(status).user(user).balance(balance)
                .build();
    }

    private LedgerAccount buildLedgerAccount(Long id, Wallet wallet) {
        return LedgerAccount.builder()
                .id(id).accountName("WALLET:" + wallet.getWalletNumber())
                .accountType(AccountType.ASSET).wallet(wallet)
                .balance(wallet.getBalance())
                .build();
    }
}
