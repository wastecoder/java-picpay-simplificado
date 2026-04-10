package com.wastecoder.picpay.user.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class IncorrectPasswordException extends ApplicationException {

    public IncorrectPasswordException() {
        super(HttpStatus.PRECONDITION_FAILED, "Incorrect password.");
    }

    public IncorrectPasswordException(String message) {
        super(HttpStatus.PRECONDITION_FAILED, message);
    }
}