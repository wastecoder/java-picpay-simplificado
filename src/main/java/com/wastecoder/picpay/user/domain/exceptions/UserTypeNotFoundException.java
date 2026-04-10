package com.wastecoder.picpay.user.domain.exceptions;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.enums.UserType;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UserTypeNotFoundException extends ApplicationException {

    public UserTypeNotFoundException() {
        super(
                HttpStatus.BAD_REQUEST,
                "User type informed not found. Values: " +
                        Arrays.stream(UserType.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))
        );
    }

    public UserTypeNotFoundException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}