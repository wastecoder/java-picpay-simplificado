package com.wastecoder.picpay.user.domain.model;

import com.wastecoder.picpay.user.domain.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record User(

        @NotBlank(message = "Full name must not be blank")
        @Size(min = 5, max = 32, message = "Full name must be between 5 and 32 characters")
        String fullName,

        @NotBlank(message = "Document must not be blank")
        @Pattern(
                regexp = "(^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$)|(^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$)",
                message = "Invalid document format"
        )
        String document,

        @NotBlank(message = "Email must not be blank")
        @Pattern(
                regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
                message = "Invalid email format"
        )
        String email,

        @NotBlank(message = "Password must not be blank")
        String password,

        @NotNull(message = "User type must not be null")
        UserType type,

        BigDecimal balance,
        UUID id
) {
    public User {
        balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public User(String fullName, String document, String email, String password, UserType type) {
        this(fullName, document, email, password, type, BigDecimal.ZERO, null);
    }

    public User withPassword(String newPassword) {
        return new User(fullName, document, email, newPassword, type, balance, id);
    }
}
