package com.wastecoder.picpay.transaction.domain.ports.input;

import com.wastecoder.picpay.transaction.domain.viewmodels.TransferCommand;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferResult;

public interface TransferUseCase {

    TransferResult execute(TransferCommand command);
}
