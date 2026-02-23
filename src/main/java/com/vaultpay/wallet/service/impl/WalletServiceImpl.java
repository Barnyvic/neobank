package com.vaultpay.wallet.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.ledger.entity.LedgerAccount;
import com.vaultpay.ledger.enums.AccountType;
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
import com.vaultpay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vaultpay.common.config.CacheConfig.*;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final int WALLET_NUMBER_LENGTH = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    @CacheEvict(value = WALLETS_BY_USER, key = "#userId")
    public WalletResponse createWallet(Long userId, CreateWalletRequest request) {
        Currency currency;
        try {
            currency = Currency.valueOf(request.currency().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unsupported currency: " + request.currency(), HttpStatus.BAD_REQUEST);
        }

        if (walletRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new DuplicateResourceException("Wallet", currency.name());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        String walletNumber = generateUniqueWalletNumber();

        Wallet wallet = Wallet.builder()
                .user(user)
                .walletNumber(walletNumber)
                .currency(currency)
                .build();
        wallet = walletRepository.save(wallet);

        LedgerAccount ledgerAccount = LedgerAccount.builder()
                .accountName("WALLET:" + walletNumber)
                .accountType(AccountType.ASSET)
                .wallet(wallet)
                .build();
        ledgerAccountRepository.save(ledgerAccount);

        log.debug("Created wallet {} for user {}", walletNumber, userId);
        return WalletResponse.from(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WALLET_BY_ID, key = "#walletId")
    public WalletResponse getWalletById(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));
        return WalletResponse.from(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WALLETS_BY_USER, key = "#userId")
    public List<WalletResponse> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(WalletResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WALLET_BALANCE, key = "#walletId")
    public BalanceResponse getBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));
        return BalanceResponse.from(wallet);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = WALLET_BY_ID, key = "#walletId"),
            @CacheEvict(value = WALLET_BALANCE, key = "#walletId")
    })
    public void freezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new BusinessException("Wallet is already frozen", HttpStatus.CONFLICT, ErrorCode.CONFLICT);
        }

        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(wallet);
        log.debug("Froze wallet {}", walletId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = WALLET_BY_ID, key = "#walletId"),
            @CacheEvict(value = WALLET_BALANCE, key = "#walletId")
    })
    public void unfreezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new BusinessException("Wallet is not frozen", HttpStatus.CONFLICT, ErrorCode.CONFLICT);
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        log.debug("Unfroze wallet {}", walletId);
    }

    private String generateUniqueWalletNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(WALLET_NUMBER_LENGTH);
            for (int i = 0; i < WALLET_NUMBER_LENGTH; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            String number = sb.toString();
            if (walletRepository.findByWalletNumber(number).isEmpty()) {
                return number;
            }
        }
        throw new BusinessException("Unable to generate unique wallet number", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
