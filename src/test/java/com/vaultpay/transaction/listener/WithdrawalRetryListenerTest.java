package com.vaultpay.transaction.listener;

import com.vaultpay.common.event.WithdrawalFailedEvent;
import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.paystack.dto.InitiateTransferResponse;
import com.vaultpay.paystack.dto.TransferRecipientResponse;
import com.vaultpay.paystack.service.PaystackService;
import com.vaultpay.transaction.entity.Transaction;
import com.vaultpay.transaction.enums.TransactionStatus;
import com.vaultpay.transaction.enums.TransactionType;
import com.vaultpay.transaction.repository.TransactionRepository;
import com.vaultpay.transaction.service.RefundService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalRetryListener Tests")
class WithdrawalRetryListenerTest {

    @Mock
    private PaystackService paystackService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RefundService refundService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private WithdrawalRetryListener listener;

    private WithdrawalFailedEvent createEvent(int attemptNumber) {
        return new WithdrawalFailedEvent(
                this, 100L, "WTH-TEST", "058", "0123456789",
                BigDecimal.valueOf(5000), "NGN", attemptNumber);
    }

    @Nested
    @DisplayName("onWithdrawalFailed")
    class OnWithdrawalFailed {

        @Test
        @DisplayName("should retry and update to PROCESSING on success")
        void shouldRetryAndUpdateOnSuccess() {
            WithdrawalFailedEvent event = createEvent(1);

            TransferRecipientResponse.RecipientData recipientData = new TransferRecipientResponse.RecipientData();
            recipientData.setRecipientCode("RCP_test123");
            TransferRecipientResponse recipientResponse = new TransferRecipientResponse();
            recipientResponse.setStatus(true);
            recipientResponse.setData(recipientData);

            InitiateTransferResponse.TransferData transferData = new InitiateTransferResponse.TransferData();
            transferData.setTransferCode("TRF_test123");
            InitiateTransferResponse transferResponse = new InitiateTransferResponse();
            transferResponse.setStatus(true);
            transferResponse.setData(transferData);

            when(paystackService.createTransferRecipient("058", "0123456789", "VaultPay User"))
                    .thenReturn(recipientResponse);
            when(paystackService.initiateTransfer(eq("RCP_test123"), any(), eq("WTH-TEST"), anyString(), eq("NGN")))
                    .thenReturn(transferResponse);

            Transaction txn = Transaction.builder()
                    .id(100L).reference("WTH-TEST").status(TransactionStatus.EXTERNAL_FAILED)
                    .transactionType(TransactionType.WITHDRAWAL).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();
            when(transactionRepository.findByReference("WTH-TEST")).thenReturn(Optional.of(txn));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            listener.onWithdrawalFailed(event);

            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
            verify(refundService, never()).reverseTransaction(any());
        }

        @Test
        @DisplayName("should auto-refund when max retries exceeded")
        void shouldAutoRefundWhenMaxRetriesExceeded() {
            WithdrawalFailedEvent event = createEvent(4);

            Transaction txn = Transaction.builder()
                    .id(100L).reference("WTH-TEST").status(TransactionStatus.EXTERNAL_FAILED)
                    .transactionType(TransactionType.WITHDRAWAL).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();
            when(transactionRepository.findByReference("WTH-TEST")).thenReturn(Optional.of(txn));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            listener.onWithdrawalFailed(event);

            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
            verify(refundService).reverseTransaction("WTH-TEST");
            verify(paystackService, never()).createTransferRecipient(any(), any(), any());
        }

        @Test
        @DisplayName("should schedule retry via TaskScheduler on non-final failure")
        void shouldScheduleRetryOnNonFinalFailure() {
            WithdrawalFailedEvent event = createEvent(1);

            when(paystackService.createTransferRecipient(any(), any(), any()))
                    .thenThrow(new BusinessException("Paystack down", HttpStatus.SERVICE_UNAVAILABLE));

            listener.onWithdrawalFailed(event);

            verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
            verify(refundService, never()).reverseTransaction(any());
        }

        @Test
        @DisplayName("should auto-refund on final retry failure")
        void shouldAutoRefundOnFinalRetryFailure() {
            WithdrawalFailedEvent event = createEvent(3);

            when(paystackService.createTransferRecipient(any(), any(), any()))
                    .thenThrow(new BusinessException("Paystack down", HttpStatus.SERVICE_UNAVAILABLE));

            Transaction txn = Transaction.builder()
                    .id(100L).reference("WTH-TEST").status(TransactionStatus.EXTERNAL_FAILED)
                    .transactionType(TransactionType.WITHDRAWAL).amount(BigDecimal.valueOf(5000))
                    .currency("NGN").build();
            when(transactionRepository.findByReference("WTH-TEST")).thenReturn(Optional.of(txn));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            listener.onWithdrawalFailed(event);

            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
            verify(refundService).reverseTransaction("WTH-TEST");
            verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        @DisplayName("scheduled retry should publish event with incremented attempt number")
        void scheduledRetryShouldPublishWithIncrementedAttempt() {
            WithdrawalFailedEvent event = createEvent(2);

            when(paystackService.createTransferRecipient(any(), any(), any()))
                    .thenThrow(new BusinessException("Paystack down", HttpStatus.SERVICE_UNAVAILABLE));

            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

            listener.onWithdrawalFailed(event);

            verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

            runnableCaptor.getValue().run();

            ArgumentCaptor<WithdrawalFailedEvent> eventCaptor = ArgumentCaptor.forClass(WithdrawalFailedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getAttemptNumber()).isEqualTo(3);
            assertThat(eventCaptor.getValue().getReference()).isEqualTo("WTH-TEST");
        }
    }
}
