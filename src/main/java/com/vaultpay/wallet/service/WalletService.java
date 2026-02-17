package com.vaultpay.wallet.service;

import com.vaultpay.wallet.dto.request.CreateWalletRequest;
import com.vaultpay.wallet.dto.response.BalanceResponse;
import com.vaultpay.wallet.dto.response.WalletResponse;

import java.util.List;

public interface WalletService {

    WalletResponse createWallet(Long userId, CreateWalletRequest request);

    WalletResponse getWalletById(Long walletId);

    List<WalletResponse> getWalletsByUserId(Long userId);

    BalanceResponse getBalance(Long walletId);

    void freezeWallet(Long walletId);

    void unfreezeWallet(Long walletId);
}
