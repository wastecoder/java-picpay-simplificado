package com.wastecoder.picpay.user.adapter.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotifyUserRequest(

        @JsonProperty("email")
        String email,

        @JsonProperty("message_title")
        String messageTitle,

        @JsonProperty("message_body")
        String messageBody

) {}
