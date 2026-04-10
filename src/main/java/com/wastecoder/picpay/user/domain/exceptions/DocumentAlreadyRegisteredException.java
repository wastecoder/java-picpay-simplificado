package com.wastecoder.picpay.user.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class DocumentAlreadyRegisteredException extends ApplicationException {

    public DocumentAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "Document already registered to another user.");
    }

    public DocumentAlreadyRegisteredException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}