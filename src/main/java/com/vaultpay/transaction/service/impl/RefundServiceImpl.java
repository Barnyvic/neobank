package com.vaultpay.transaction.service.impl;

import com.vaultpay.common.event.TransactionReversedEvent;
import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.ledger.entity.JournalEntry;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.ledger.service.LedgerService;
import com.vaultpay.transaction.dto.response.TransactionResponse;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.RefundService;
import com.vaultpay.wallet.entity.Wallet;
import com.vaultpay.wallet.repository.WalletRepository;
import com.vaultpay.wallet.service.WalletCacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vaultpay.common.util.ReferenceGenerator.generate;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final String PAYSTACK_SYSTEM_ACCOUNT = "PAYSTACK_LIABILITY";
    private static final Set<TransactionStatus> REVERSIBLE_STATUSES =
            Set.of(TransactionStatus.EXTERNAL_FAILED, TransactionStatus.FAILED, TransactionStatus.PENDING_EXTERNAL);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerService ledgerService;
    private final WalletCacheEvictionService walletCacheEvictionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransactionResponse reverseTransaction(String reference) {
        Transaction original = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));

        if (original.getStatus() == TransactionStatus.REVERSED) {
            log.info("Transaction already reversed: ref={}", reference);
            return TransactionResponse.from(original);
        }

        if (!REVERSIBLE_STATUSES.contains(original.getStatus())) {
            throw new BusinessException(
                    "Transaction cannot be reversed (status=" + original.getStatus() + ")",
                    HttpStatus.CONFLICT, ErrorCode.REVERSAL_FAILED);
        }

        Wallet wallet = resolveSourceWallet(original);
        Wallet lockedWallet = walletRepository.findByIdWithLock(wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", wallet.getId().toString()));

        LedgerAccount userAccount = ledgerAccountRepository.findByWalletId(lockedWallet.getId())
                .orElseThrow(() -> new BusinessException(
                        "Ledger account not found for wallet", HttpStatus.INTERNAL_SERVER_ERROR));
        LedgerAccount systemAccount = ledgerService.getOrCreateSystemAccount(PAYSTACK_SYSTEM_ACCOUNT);

        String reversalRef = generate("REV");
        String description = "Reversal of " + original.getReference();

        JournalEntry journal = ledgerService.createReversalEntry(
                userAccount, systemAccount, original.getAmount(), reversalRef, description);

        Transaction reversal = Transaction.builder()
                .reference(reversalRef)
                .transactionType(TransactionType.REVERSAL)
                .status(TransactionStatus.COMPLETED)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .description(description)
                .destWallet(lockedWallet)
                .journalEntry(journal)
                .originalTransaction(original)
                .build();
        transactionRepository.save(reversal);

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        Long userId = lockedWallet.getUser().getId();
        walletCacheEvictionService.evictWalletCaches(lockedWallet.getId(), userId);

        eventPublisher.publishEvent(
                new TransactionReversedEvent(this, reference, reversalRef, lockedWallet.getId()));
        log.info("Transaction reversed: original={}, reversal={}, amount={}",
                reference, reversalRef, original.getAmount());

        return TransactionResponse.from(reversal);
    }

    private Wallet resolveSourceWallet(Transaction transaction) {
        if (transaction.getSourceWallet() != null) {
            return transaction.getSourceWallet();
        }
        if (transaction.getDestWallet() != null) {
            return transaction.getDestWallet();
        }
        throw new BusinessException("No wallet linked to transaction " + transaction.getReference(),
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.REVERSAL_FAILED);
    }

}
