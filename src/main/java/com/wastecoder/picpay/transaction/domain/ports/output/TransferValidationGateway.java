package com.wastecoder.picpay.transaction.domain.ports.output;

import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferValidationResult;

public interface TransferValidationGateway {

    TransferValidationResult validate(Transaction transaction);
}
