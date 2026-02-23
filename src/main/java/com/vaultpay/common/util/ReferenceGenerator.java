package com.vaultpay.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class ReferenceGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int WALLET_NUMBER_LENGTH = 10;

    private ReferenceGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static String generateWalletNumber() {
        StringBuilder sb = new StringBuilder(WALLET_NUMBER_LENGTH);
        for (int i = 0; i < WALLET_NUMBER_LENGTH; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
