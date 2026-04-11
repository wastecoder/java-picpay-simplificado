package com.wastecoder.picpay.user.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.ports.input.LoginUserUseCase;

import java.util.UUID;

public record LoginUserResponse(

        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn

) {

    public LoginUserResponse(LoginUserUseCase.LoginUserResult result) {
        this(
                result.userId(),
                result.accessToken(),
                result.expiresIn()
        );
    }
}
