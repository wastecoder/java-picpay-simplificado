package com.wastecoder.picpay.transaction.domain.model;

import com.wastecoder.picpay.user.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record Transaction(

        @NotNull(message = "Sender (from) must not be null")
        User from,

        @NotNull(message = "Target must not be null")
        User target,

        @NotNull(message = "Value must not be null")
        @Positive(message = "Value must be positive")
        BigDecimal value,

        @NotBlank(message = "Description must not be blank")
        String description
) {}
