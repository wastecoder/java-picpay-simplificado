package com.wastecoder.picpay.common.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ApplicationException extends ResponseStatusException {

    public ApplicationException(HttpStatus status, String message) {
        super(status, message);
    }

    public ApplicationException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}