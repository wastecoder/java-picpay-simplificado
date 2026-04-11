package com.wastecoder.picpay.transaction.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class UserCantTransferException extends ApplicationException {

    public UserCantTransferException() {
        super(HttpStatus.PRECONDITION_FAILED, "This user cannot send transfers.");
    }

    public UserCantTransferException(String message) {
        super(HttpStatus.PRECONDITION_FAILED, message);
    }
}
