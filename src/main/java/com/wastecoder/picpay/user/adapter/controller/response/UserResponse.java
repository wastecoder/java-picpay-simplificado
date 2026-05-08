package com.wastecoder.picpay.user.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.model.User;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(

        @JsonProperty("id")
        UUID id,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("document")
        String document,

        @JsonProperty("email")
        String email,

        @JsonProperty("type")
        UserType type,

        @JsonProperty("balance")
        BigDecimal balance

) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.fullName(),
                user.document(),
                user.email(),
                user.type(),
                user.balance()
        );
    }
}
