package com.wastecoder.picpay.user.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApplicationException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "Email already registered to another user.");
    }

    public EmailAlreadyRegisteredException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}