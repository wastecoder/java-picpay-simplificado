package com.wastecoder.picpay.common.adapter.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotifyMessage(

        @JsonProperty("email")
        String email,

        @JsonProperty("message_title")
        String messageTitle,

        @JsonProperty("message_body")
        String messageBody

) {}
