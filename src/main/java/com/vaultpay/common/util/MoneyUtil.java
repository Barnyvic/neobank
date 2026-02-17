package com.vaultpay.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private MoneyUtil() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean hasSufficientFunds(BigDecimal balance, BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
