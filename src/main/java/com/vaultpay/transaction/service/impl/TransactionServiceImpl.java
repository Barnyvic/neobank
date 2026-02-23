package com.vaultpay.transaction.service.impl;

import com.vaultpay.common.event.TransactionCompletedEvent;
import com.vaultpay.common.event.WalletFundedEvent;
import com.vaultpay.common.exception.*;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.service.LedgerService;
import com.vaultpay.paystack.dto.InitializePaymentResponse;
import com.vaultpay.paystack.service.PaystackService;
import com.vaultpay.transaction.dto.request.FundWalletRequest;
import com.vaultpay.transaction.dto.request.TransferRequest;
import com.vaultpay.transaction.dto.request.WithdrawRequest;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.TransactionLockService;
import com.vaultpay.transaction.service.TransactionService;
import com.vaultpay.transaction.service.TransactionThrottleService;
import com.vaultpay.user.service.UserService;
import com.vaultpay.wallet.entity.Wallet;
import com.vaultpay.wallet.enums.WalletStatus;
import com.vaultpay.wallet.repository.WalletRepository;
import com.vaultpay.wallet.service.WalletCacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vaultpay.common.config.CacheConfig.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String PAYSTACK_SYSTEM_ACCOUNT = "PAYSTACK_LIABILITY";

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerService ledgerService;
    private final UserService userService;
    private final PaystackService paystackService;
    private final TransactionThrottleService throttleService;
    private final TransactionLockService lockService;
    private final WalletCacheEvictionService walletCacheEvictionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransactionResponse transfer(Long userId, TransferRequest request) {
        Optional<Transaction> existing = findExistingByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }

        if (!userService.verifyTransactionPin(userId, request.transactionPin())) {
            throw new BusinessException("Invalid transaction PIN", HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        Wallet sourceWallet = walletRepository.findByUserId(userId).stream()
                .filter(w -> w.getStatus() == WalletStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active wallet", userId.toString()));

        validateWalletActive(sourceWallet, "Source");

        Wallet destWallet = walletRepository.findByWalletNumber(request.recipientWalletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", request.recipientWalletNumber()));
        validateWalletActive(destWallet, "Destination");

        if (sourceWallet.getId().equals(destWallet.getId())) {
            throw new BusinessException("Cannot transfer to the same wallet", HttpStatus.BAD_REQUEST);
        }

        if (!throttleService.checkAndMark(userId, request.amount(), request.recipientWalletNumber())) {
            throw new BusinessException(
                    "Duplicate transaction detected. Please wait 30 seconds before retrying the same transfer.",
                    HttpStatus.TOO_MANY_REQUESTS, ErrorCode.DUPLICATE_TRANSACTION);
        }

        if (!lockService.acquireLock(userId, sourceWallet.getId())) {
            throw new BusinessException(
                    "A transaction is already in progress for this wallet. Please wait.",
                    HttpStatus.CONFLICT, ErrorCode.TRANSACTION_IN_PROGRESS);
        }

        try {
            Wallet lockedSource = walletRepository.findByIdWithLock(sourceWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", sourceWallet.getId().toString()));

            if (lockedSource.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException();
            }

            LedgerAccount sourceAccount = ledgerAccountRepository.findByWalletId(lockedSource.getId())
                    .orElseThrow(() -> new BusinessException("Ledger account not found for source wallet", HttpStatus.INTERNAL_SERVER_ERROR));
            LedgerAccount destAccount = ledgerAccountRepository.findByWalletId(destWallet.getId())
                    .orElseThrow(() -> new BusinessException("Ledger account not found for destination wallet", HttpStatus.INTERNAL_SERVER_ERROR));

            String reference = generateReference("TRF");
            String description = request.description() != null ? request.description() : "Transfer to " + destWallet.getWalletNumber();

            JournalEntry journal = ledgerService.createTransferEntry(sourceAccount, destAccount, request.amount(), reference, description);

            lockedSource.setBalance(lockedSource.getBalance().subtract(request.amount()));
            walletRepository.save(lockedSource);

            Wallet lockedDest = walletRepository.findByIdWithLock(destWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", destWallet.getId().toString()));
            lockedDest.setBalance(lockedDest.getBalance().add(request.amount()));
            walletRepository.save(lockedDest);

            Transaction transaction = Transaction.builder()
                    .reference(reference)
                    .transactionType(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .amount(request.amount())
                    .currency(lockedSource.getCurrency().name())
                    .description(description)
                    .sourceWallet(lockedSource)
                    .destWallet(lockedDest)
                    .journalEntry(journal)
                    .idempotencyKey(request.idempotencyKey())
                    .build();
            transaction = transactionRepository.save(transaction);

            walletCacheEvictionService.evictWalletCaches(lockedSource.getId(), userId);
            walletCacheEvictionService.evictWalletCaches(lockedDest.getId(), lockedDest.getUser().getId());

            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction.getId(), reference));
            log.info("Transfer completed: ref={}, amount={}, {} -> {}", reference, request.amount(),
                    lockedSource.getWalletNumber(), lockedDest.getWalletNumber());

            return TransactionResponse.from(transaction);
        } finally {
            lockService.releaseLock(userId, sourceWallet.getId());
        }
    }

    @Override
    @Transactional
    public TransactionResponse initiateFunding(Long userId, FundWalletRequest request) {
        Optional<Transaction> existing = findExistingByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }

        Wallet wallet = walletRepository.findById(request.walletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", request.walletId().toString()));
        validateWalletActive(wallet, "Funding target");
        validateWalletOwnership(wallet, userId);

        String reference = generateReference("FND");

        Transaction transaction = Transaction.builder()
                .reference(reference)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .amount(request.amount())
                .currency(wallet.getCurrency().name())
                .description("Wallet funding via Paystack")
                .destWallet(wallet)
                .idempotencyKey(request.idempotencyKey())
                .build();
        transaction = transactionRepository.save(transaction);

        String email = wallet.getUser().getEmail();
        BigDecimal amountInKobo = request.amount().multiply(BigDecimal.valueOf(100));
        InitializePaymentResponse paymentResponse = paystackService.initializePayment(
                email, amountInKobo, reference, wallet.getCurrency().name());

        String authUrl = paymentResponse.getData() != null ? paymentResponse.getData().getAuthorizationUrl() : null;

        log.info("Funding initiated: ref={}, amount={}, wallet={}", reference, request.amount(), wallet.getWalletNumber());
        return TransactionResponse.from(transaction, authUrl);
    }

    @Override
    @Transactional
    public TransactionResponse completeFunding(String reference, String paystackReference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            return TransactionResponse.from(transaction);
        }

        Wallet wallet = walletRepository.findByIdWithLock(transaction.getDestWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", transaction.getDestWallet().getId().toString()));

        LedgerAccount userAccount = ledgerAccountRepository.findByWalletId(wallet.getId())
                .orElseThrow(() -> new BusinessException("Ledger account not found for wallet", HttpStatus.INTERNAL_SERVER_ERROR));
        LedgerAccount paystackAccount = ledgerService.getOrCreateSystemAccount(PAYSTACK_SYSTEM_ACCOUNT);

        JournalEntry journal = ledgerService.createFundingEntry(
                userAccount, paystackAccount, transaction.getAmount(), reference, "Paystack funding: " + paystackReference);

        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setJournalEntry(journal);
        transaction.setMetadata("{\"paystackReference\":\"" + paystackReference + "\"}");
        transactionRepository.save(transaction);

        walletCacheEvictionService.evictWalletCaches(wallet.getId(), wallet.getUser().getId());

        eventPublisher.publishEvent(new WalletFundedEvent(this, wallet.getId(), transaction.getAmount(), reference));
        log.info("Funding completed: ref={}, amount={}, wallet={}", reference, transaction.getAmount(), wallet.getWalletNumber());

        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        Optional<Transaction> existing = findExistingByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }

        if (!userService.verifyTransactionPin(userId, request.transactionPin())) {
            throw new BusinessException("Invalid transaction PIN", HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        Wallet wallet = walletRepository.findById(request.walletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", request.walletId().toString()));
        validateWalletActive(wallet, "Withdrawal source");
        validateWalletOwnership(wallet, userId);

        if (!lockService.acquireLock(userId, wallet.getId())) {
            throw new BusinessException(
                    "A transaction is already in progress for this wallet. Please wait.",
                    HttpStatus.CONFLICT, ErrorCode.TRANSACTION_IN_PROGRESS);
        }

        try {
            Wallet lockedWallet = walletRepository.findByIdWithLock(wallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", wallet.getId().toString()));

            if (lockedWallet.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException();
            }

            LedgerAccount userAccount = ledgerAccountRepository.findByWalletId(lockedWallet.getId())
                    .orElseThrow(() -> new BusinessException("Ledger account not found for wallet", HttpStatus.INTERNAL_SERVER_ERROR));
            LedgerAccount paystackAccount = ledgerService.getOrCreateSystemAccount(PAYSTACK_SYSTEM_ACCOUNT);

            String reference = generateReference("WTH");
            String description = "Withdrawal to bank " + request.bankCode() + " - " + request.accountNumber();

            JournalEntry journal = ledgerService.createWithdrawalEntry(
                    userAccount, paystackAccount, request.amount(), reference, description);

            lockedWallet.setBalance(lockedWallet.getBalance().subtract(request.amount()));
            walletRepository.save(lockedWallet);

            Transaction transaction = Transaction.builder()
                    .reference(reference)
                    .transactionType(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.COMPLETED)
                    .amount(request.amount())
                    .currency(lockedWallet.getCurrency().name())
                    .description(description)
                    .sourceWallet(lockedWallet)
                    .journalEntry(journal)
                    .idempotencyKey(request.idempotencyKey())
                    .metadata("{\"bankCode\":\"" + request.bankCode() + "\",\"accountNumber\":\"" + request.accountNumber() + "\"}")
                    .build();
            transaction = transactionRepository.save(transaction);

            walletCacheEvictionService.evictWalletCaches(lockedWallet.getId(), userId);

            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction.getId(), reference));
            log.info("Withdrawal completed: ref={}, amount={}, wallet={}", reference, request.amount(), lockedWallet.getWalletNumber());

            return TransactionResponse.from(transaction);
        } finally {
            lockService.releaseLock(userId, wallet.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = TRANSACTION_BY_REF, key = "#reference")
    public TransactionResponse getTransaction(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable) {
        return transactionRepository
                .findBySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(walletId, walletId, pageable)
                .map(TransactionResponse::from);
    }

    private Optional<Transaction> findExistingByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    private void validateWalletActive(Wallet wallet, String label) {
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new BusinessException(label + " wallet is frozen", HttpStatus.FORBIDDEN, ErrorCode.WALLET_FROZEN);
        }
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new BusinessException(label + " wallet is closed", HttpStatus.FORBIDDEN, ErrorCode.WALLET_FROZEN);
        }
    }

    private void validateWalletOwnership(Wallet wallet, Long userId) {
        if (!wallet.getUser().getId().equals(userId)) {
            throw new BusinessException("Wallet does not belong to user", HttpStatus.FORBIDDEN, ErrorCode.UNAUTHORIZED);
        }
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
