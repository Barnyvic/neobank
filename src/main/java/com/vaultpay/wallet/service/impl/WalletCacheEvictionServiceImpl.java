package com.vaultpay.wallet.service.impl;

import com.vaultpay.wallet.service.WalletCacheEvictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.vaultpay.common.config.CacheConfig.*;

@Service
@RequiredArgsConstructor
public class WalletCacheEvictionServiceImpl implements WalletCacheEvictionService {

    private final CacheManager cacheManager;

    @Override
    public void evictWalletCaches(Long walletId, Long userId) {
        Objects.requireNonNull(cacheManager.getCache(WALLET_BY_ID)).evict(walletId);
        Objects.requireNonNull(cacheManager.getCache(WALLET_BALANCE)).evict(walletId);
        Objects.requireNonNull(cacheManager.getCache(WALLETS_BY_USER)).evict(userId);
    }
}
