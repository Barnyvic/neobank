package com.vaultpay.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ReferenceGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ReferenceGenerator() {
    }

    public static String generateTransactionReference() {
        return "TXN-" + LocalDateTime.now().format(FORMATTER) + "-" + shortUuid();
    }

    public static String generateDepositReference() {
        return "DEP-" + LocalDateTime.now().format(FORMATTER) + "-" + shortUuid();
    }

    public static String generateWithdrawalReference() {
        return "WDR-" + LocalDateTime.now().format(FORMATTER) + "-" + shortUuid();
    }

    public static String generateWalletNumber() {
        return "VP" + System.currentTimeMillis();
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
