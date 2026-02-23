package com.vaultpay.wallet.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.repository.LedgerAccountRepository;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.repository.UserRepository;
import com.vaultpay.wallet.dto.request.CreateWalletRequest;
import com.vaultpay.wallet.dto.response.BalanceResponse;
import com.vaultpay.wallet.dto.response.WalletResponse;
import com.vaultpay.wallet.entity.Wallet;
import com.vaultpay.wallet.enums.Currency;
import com.vaultpay.wallet.enums.WalletStatus;
import com.vaultpay.wallet.repository.WalletRepository;
import com.vaultpay.wallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("createWallet")
    class CreateWallet {

        @Test
        @DisplayName("should create wallet and linked ledger account")
        void shouldCreateWalletSuccessfully() {
            CreateWalletRequest request = new CreateWalletRequest("NGN");
            User user = User.builder().id(USER_ID).build();
            Wallet savedWallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.ACTIVE, user);

            when(walletRepository.existsByUserIdAndCurrency(USER_ID, Currency.NGN)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(walletRepository.findByWalletNumber(any())).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);
            when(ledgerAccountRepository.save(any(LedgerAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            WalletResponse response = walletService.createWallet(USER_ID, request);

            assertThat(response.walletNumber()).isEqualTo("1234567890");
            assertThat(response.currency()).isEqualTo("NGN");
            verify(ledgerAccountRepository).save(any(LedgerAccount.class));
        }

        @Test
        @DisplayName("should reject duplicate currency wallet for user")
        void shouldRejectDuplicateCurrency() {
            CreateWalletRequest request = new CreateWalletRequest("NGN");
            when(walletRepository.existsByUserIdAndCurrency(USER_ID, Currency.NGN)).thenReturn(true);

            assertThatThrownBy(() -> walletService.createWallet(USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should reject invalid currency")
        void shouldRejectInvalidCurrency() {
            CreateWalletRequest request = new CreateWalletRequest("INVALID");

            assertThatThrownBy(() -> walletService.createWallet(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unsupported currency");
        }
    }

    @Nested
    @DisplayName("getWalletById")
    class GetWalletById {

        @Test
        @DisplayName("should return wallet when found")
        void shouldReturnWallet() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.ACTIVE, null);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            WalletResponse response = walletService.getWalletById(1L);

            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw when wallet not found")
        void shouldThrowWhenNotFound() {
            when(walletRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getWalletById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getWalletsByUserId")
    class GetWalletsByUserId {

        @Test
        @DisplayName("should return all wallets for user")
        void shouldReturnUserWallets() {
            Wallet wallet1 = buildWallet(1L, "1111111111", Currency.NGN, WalletStatus.ACTIVE, null);
            Wallet wallet2 = buildWallet(2L, "2222222222", Currency.USD, WalletStatus.ACTIVE, null);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(List.of(wallet1, wallet2));

            List<WalletResponse> result = walletService.getWalletsByUserId(USER_ID);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("should return balance from ledger account")
        void shouldReturnBalance() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.ACTIVE, null);
            LedgerAccount ledgerAccount = LedgerAccount.builder()
                    .id(10L).accountName("WALLET:1234567890")
                    .balance(BigDecimal.valueOf(5000)).wallet(wallet).build();
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
            when(ledgerAccountRepository.findByWalletId(1L)).thenReturn(Optional.of(ledgerAccount));

            BalanceResponse response = walletService.getBalance(1L);

            assertThat(response.availableBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }
    }

    @Nested
    @DisplayName("freezeWallet")
    class FreezeWallet {

        @Test
        @DisplayName("should freeze an active wallet")
        void shouldFreezeWallet() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.ACTIVE, null);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            walletService.freezeWallet(1L);

            assertThat(wallet.getStatus()).isEqualTo(WalletStatus.FROZEN);
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("should throw when freezing already frozen wallet")
        void shouldThrowWhenAlreadyFrozen() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.FROZEN, null);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.freezeWallet(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already frozen");
        }
    }

    @Nested
    @DisplayName("unfreezeWallet")
    class UnfreezeWallet {

        @Test
        @DisplayName("should unfreeze a frozen wallet")
        void shouldUnfreezeWallet() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.FROZEN, null);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            walletService.unfreezeWallet(1L);

            assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("should throw when unfreezing non-frozen wallet")
        void shouldThrowWhenNotFrozen() {
            Wallet wallet = buildWallet(1L, "1234567890", Currency.NGN, WalletStatus.ACTIVE, null);
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.unfreezeWallet(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not frozen");
        }
    }

    private Wallet buildWallet(Long id, String number, Currency currency, WalletStatus status, User user) {
        return Wallet.builder()
                .id(id)
                .walletNumber(number)
                .currency(currency)
                .status(status)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build();
    }
}
