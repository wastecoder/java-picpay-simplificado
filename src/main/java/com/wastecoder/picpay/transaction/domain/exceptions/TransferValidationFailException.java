package com.wastecoder.picpay.transaction.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class TransferValidationFailException extends ApplicationException {

    public TransferValidationFailException(Exception exception) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Transfer validation request failed.", exception);
    }

    public TransferValidationFailException(String message, Exception exception) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, exception);
    }
}
