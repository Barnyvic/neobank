package com.vaultpay.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilTest {

    @Nested
    @DisplayName("scale")
    class Scale {

        @Test
        @DisplayName("should scale to 4 decimal places using HALF_EVEN rounding")
        void shouldScaleTo4DecimalPlaces() {
            assertThat(MoneyUtil.scale(new BigDecimal("100"))).isEqualByComparingTo("100.0000");
            assertThat(MoneyUtil.scale(new BigDecimal("100")).scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("should round 0.12345 using HALF_EVEN")
        void shouldRoundHalfEven() {
            assertThat(MoneyUtil.scale(new BigDecimal("0.12345"))).isEqualByComparingTo("0.1234");
        }

        @Test
        @DisplayName("should preserve amounts that already have 4 decimal places")
        void shouldPreserveExistingScale() {
            BigDecimal input = new BigDecimal("500.5000");
            BigDecimal result = MoneyUtil.scale(input);
            assertThat(result).isEqualByComparingTo("500.5000");
            assertThat(result.scale()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("isPositive")
    class IsPositive {

        @Test
        @DisplayName("should return true for positive amounts")
        void shouldReturnTrueForPositive() {
            assertThat(MoneyUtil.isPositive(new BigDecimal("0.01"))).isTrue();
            assertThat(MoneyUtil.isPositive(new BigDecimal("1000"))).isTrue();
        }

        @Test
        @DisplayName("should return false for zero")
        void shouldReturnFalseForZero() {
            assertThat(MoneyUtil.isPositive(BigDecimal.ZERO)).isFalse();
        }

        @Test
        @DisplayName("should return false for negative amounts")
        void shouldReturnFalseForNegative() {
            assertThat(MoneyUtil.isPositive(new BigDecimal("-1"))).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            assertThat(MoneyUtil.isPositive(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasSufficientFunds")
    class HasSufficientFunds {

        @Test
        @DisplayName("should return true when balance exceeds amount")
        void shouldReturnTrueWhenSufficient() {
            assertThat(MoneyUtil.hasSufficientFunds(new BigDecimal("1000"), new BigDecimal("500"))).isTrue();
        }

        @Test
        @DisplayName("should return true when balance equals amount")
        void shouldReturnTrueWhenEqual() {
            assertThat(MoneyUtil.hasSufficientFunds(new BigDecimal("500"), new BigDecimal("500"))).isTrue();
        }

        @Test
        @DisplayName("should return false when balance is less than amount")
        void shouldReturnFalseWhenInsufficient() {
            assertThat(MoneyUtil.hasSufficientFunds(new BigDecimal("499"), new BigDecimal("500"))).isFalse();
        }
    }
}
