package com.vaultpay.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceGeneratorTest {

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("should create reference with given prefix")
        void shouldCreateWithPrefix() {
            String ref = ReferenceGenerator.generate("TRF");
            assertThat(ref).startsWith("TRF-");
            assertThat(ref).hasSize(4 + 32); 
        }

        @Test
        @DisplayName("should produce unique references")
        void shouldProduceUniqueReferences() {
            String ref1 = ReferenceGenerator.generate("WTH");
            String ref2 = ReferenceGenerator.generate("WTH");
            assertThat(ref1).isNotEqualTo(ref2);
        }

        @Test
        @DisplayName("should produce uppercase suffix")
        void shouldProduceUppercaseSuffix() {
            String ref = ReferenceGenerator.generate("FND");
            String suffix = ref.substring(4);
            assertThat(suffix).isEqualTo(suffix.toUpperCase());
        }
    }

    @Nested
    @DisplayName("generateWalletNumber")
    class GenerateWalletNumber {

        @Test
        @DisplayName("should generate a 10-digit numeric string")
        void shouldGenerate10Digits() {
            String number = ReferenceGenerator.generateWalletNumber();
            assertThat(number).hasSize(10);
            assertThat(number).matches("\\d{10}");
        }

        @Test
        @DisplayName("should produce unique wallet numbers")
        void shouldProduceUniqueNumbers() {
            String n1 = ReferenceGenerator.generateWalletNumber();
            String n2 = ReferenceGenerator.generateWalletNumber();
            assertThat(n1).isNotEqualTo(n2);
        }
    }
}
