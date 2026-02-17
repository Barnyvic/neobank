package com.vaultpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VaultPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultPayApplication.class, args);
    }
}
