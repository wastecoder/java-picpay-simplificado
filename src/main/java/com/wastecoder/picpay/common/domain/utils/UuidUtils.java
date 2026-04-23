package com.wastecoder.picpay.common.domain.utils;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {}

    public static UUID uuidCustomValueOf(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, fieldName + " must be a uuid.");
        }
    }
}
