package com.wastecoder.picpay.transaction.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class TransferNotAllowedException extends ApplicationException {

    public TransferNotAllowedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "This transfer was not authorized");
    }

    public TransferNotAllowedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
