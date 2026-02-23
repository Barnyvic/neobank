package com.vaultpay.transaction.listener;

import com.vaultpay.common.event.WithdrawalFailedEvent;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.paystack.dto.InitiateTransferResponse;
import com.vaultpay.paystack.dto.TransferRecipientResponse;
import com.vaultpay.paystack.service.PaystackService;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalRetryListener {

    static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 2000;

    private final PaystackService paystackService;
    private final TransactionRepository transactionRepository;
    private final RefundService refundService;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    @Async
    @EventListener
    public void onWithdrawalFailed(WithdrawalFailedEvent event) {
        String reference = event.getReference();
        int currentAttempt = event.getAttemptNumber();

        log.info("Withdrawal retry listener triggered: ref={}, attempt={}/{}", reference, currentAttempt, MAX_RETRIES);

        if (currentAttempt > MAX_RETRIES) {
            log.warn("Max retries exhausted for ref={}, initiating auto-refund", reference);
            markFailed(reference);
            refundService.reverseTransaction(reference);
            return;
        }

        try {
            TransferRecipientResponse recipientResponse = paystackService.createTransferRecipient(
                    event.getBankCode(), event.getAccountNumber(), "VaultPay User");

            BigDecimal amountInKobo = event.getAmount().multiply(BigDecimal.valueOf(100));
            paystackService.initiateTransfer(
                    recipientResponse.getData().getRecipientCode(),
                    amountInKobo, reference, "Withdrawal retry #" + currentAttempt, event.getCurrency());

            Transaction transaction = transactionRepository.findByReference(reference)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));
            transaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);

            log.info("Withdrawal retry succeeded: ref={}, attempt={}", reference, currentAttempt);
        } catch (Exception e) {
            log.error("Withdrawal retry failed: ref={}, attempt={}, error={}", reference, currentAttempt, e.getMessage());

            if (currentAttempt >= MAX_RETRIES) {
                log.warn("Final retry exhausted for ref={}, initiating auto-refund", reference);
                markFailed(reference);
                refundService.reverseTransaction(reference);
            } else {
                scheduleRetry(event, currentAttempt);
            }
        }
    }

    private void scheduleRetry(WithdrawalFailedEvent event, int currentAttempt) {
        long delay = BASE_DELAY_MS * (1L << (currentAttempt - 1));
        WithdrawalFailedEvent nextEvent = new WithdrawalFailedEvent(
                this, event.getTransactionId(), event.getReference(),
                event.getBankCode(), event.getAccountNumber(),
                event.getAmount(), event.getCurrency(), currentAttempt + 1);

        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(nextEvent),
                Instant.now().plusMillis(delay));

        log.debug("Scheduled retry {} for ref={} in {}ms", currentAttempt + 1, event.getReference(), delay);
    }

    private void markFailed(String reference) {
        transactionRepository.findByReference(reference).ifPresent(txn -> {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
        });
    }
}
