package com.wastecoder.picpay.user.domain.ports.input;

import com.wastecoder.picpay.user.domain.viewmodels.DepositCommand;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;

public interface DepositUseCase {

    DepositResult execute(DepositCommand command);
}
