package com.vaultpay.wallet.service;

public interface WalletCacheEvictionService {

    void evictWalletCaches(Long walletId, Long userId);
}
