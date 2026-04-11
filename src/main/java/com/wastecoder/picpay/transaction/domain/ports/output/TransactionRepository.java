package com.wastecoder.picpay.transaction.domain.ports.output;

import com.wastecoder.picpay.transaction.domain.model.Transaction;

public interface TransactionRepository {

    void storage(Transaction transaction);
}
