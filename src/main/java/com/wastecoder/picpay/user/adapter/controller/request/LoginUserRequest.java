package com.wastecoder.picpay.user.adapter.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(

        @JsonProperty("email")
        @NotBlank(message = "email can't be blank.")
        @Schema(example = "user@email.com")
        String email,

        @JsonProperty("password")
        @NotBlank(message = "password can't be blank.")
        @Schema(example = "password123")
        String password

) {

    public LoginUserCommand toCommand() {
        return new LoginUserCommand(
                email,
                password
        );
    }
}
