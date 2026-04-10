package com.wastecoder.picpay.user.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super(HttpStatus.PRECONDITION_FAILED, "User not found.");
    }

    public UserNotFoundException(String message) {
        super(HttpStatus.PRECONDITION_FAILED, message);
    }
}