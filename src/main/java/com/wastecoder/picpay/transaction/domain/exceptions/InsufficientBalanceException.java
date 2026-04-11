package com.wastecoder.picpay.transaction.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApplicationException {

    public InsufficientBalanceException() {
        super(HttpStatus.PRECONDITION_FAILED, "User does not have enough balance for this transaction");
    }

    public InsufficientBalanceException(String message) {
        super(HttpStatus.PRECONDITION_FAILED, message);
    }
}
