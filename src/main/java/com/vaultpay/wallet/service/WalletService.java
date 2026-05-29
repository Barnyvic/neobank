package com.vaultpay.wallet.service;

import com.vaultpay.wallet.dto.request.CreateWalletRequest;
import com.vaultpay.wallet.dto.response.BalanceResponse;
import com.vaultpay.wallet.dto.response.WalletResponse;

import java.util.List;

public interface WalletService {

    WalletResponse createWallet(Long userId, CreateWalletRequest request);

    WalletResponse getWalletById(Long walletId, Long userId);

    List<WalletResponse> getWalletsByUserId(Long userId);

    BalanceResponse getBalance(Long walletId, Long userId);

    void freezeWallet(Long walletId, Long userId);

    void unfreezeWallet(Long walletId, Long userId);
}
