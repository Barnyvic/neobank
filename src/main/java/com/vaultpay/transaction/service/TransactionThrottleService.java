package com.vaultpay.transaction.service;

import java.math.BigDecimal;

public interface TransactionThrottleService {

    boolean checkAndMark(Long userId, BigDecimal amount, String destWalletNumber);
}
