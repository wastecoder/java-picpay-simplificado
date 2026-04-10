package com.wastecoder.picpay.user.domain.model;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.enums.UserType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public class User {

    private static final Pattern DOCUMENT_PATTERN = Pattern.compile(
            "(^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$)|(^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$)"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private final String fullName;
    private final String document;
    private final String email;
    private final String password;
    private final UserType type;
    private final BigDecimal balance;
    private final UUID id;

    public User(String fullName,
                String document,
                String email,
                String password,
                UserType type,
                BigDecimal balance,
                UUID id) {

        this.fullName = fullName;
        this.document = document;
        this.email = email;
        this.password = password;
        this.type = type;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.id = id;

        validate();
    }

    public User(String fullName,
                String document,
                String email,
                String password,
                UserType type) {
        this(fullName, document, email, password, type, BigDecimal.ZERO, null);
    }

    private void validate() {

        if (fullName == null || fullName.isBlank()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Full name must not be blank"
            );
        }

        if (fullName.length() < 5 || fullName.length() > 32) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Full name must be between 5 and 32 characters"
            );
        }

        if (document == null || document.isBlank()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Document must not be blank"
            );
        }

        if (!DOCUMENT_PATTERN.matcher(document).matches()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document format"
            );
        }

        if (email == null || email.isBlank()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Email must not be blank"
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email format"
            );
        }

        if (password == null || password.isBlank()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not be blank"
            );
        }

        if (type == null) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "User type must not be null"
            );
        }
    }
}