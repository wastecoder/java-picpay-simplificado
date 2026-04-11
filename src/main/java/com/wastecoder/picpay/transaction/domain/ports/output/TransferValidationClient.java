package com.wastecoder.picpay.transaction.domain.ports.output;

import com.wastecoder.picpay.transaction.domain.model.Transaction;

public interface TransferValidationClient {

    TransferValidationResult validate(Transaction transaction);

    enum TransferValidationResult {
        ALLOWED,
        DENIED
    }
}
