package com.vaultpay.transaction.service;

public interface TransactionLockService {

    boolean acquireLock(Long userId, Long walletId);

    void releaseLock(Long userId, Long walletId);
}
