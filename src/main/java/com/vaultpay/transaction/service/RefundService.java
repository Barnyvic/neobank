package com.vaultpay.transaction.service;

import com.vaultpay.transaction.dto.response.TransactionResponse;

public interface RefundService {

    TransactionResponse reverseTransaction(String reference);
}
