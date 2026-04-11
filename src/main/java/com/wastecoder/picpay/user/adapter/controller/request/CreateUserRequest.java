package com.wastecoder.picpay.user.adapter.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.UserTypeNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(

        @JsonProperty("full_name")
        @NotBlank(message = "full_name can't be blank.")
        @Schema(example = "User Name")
        String fullName,

        @JsonProperty("document")
        @NotBlank(message = "document can't be blank.")
        @Schema(example = "000.000.000-00")
        String document,

        @JsonProperty("email")
        @NotBlank(message = "email can't be blank.")
        @Schema(example = "user@email.com")
        String email,

        @JsonProperty("password")
        @NotBlank(message = "password can't be blank.")
        @Schema(example = "password123")
        String password,

        @JsonProperty("type")
        @NotBlank(message = "type can't be blank.")
        @Schema(example = "COMMON or MERCHANT")
        String type

) {

    public User toModel() {
        return new User(
                fullName,
                document,
                email,
                password,
                UserType.findByName(type)
                        .orElseThrow(UserTypeNotFoundException::new)
        );
    }
}
