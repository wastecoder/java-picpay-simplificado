package com.wastecoder.picpay.common.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        @JsonProperty("timestamp")
        Instant timestamp,

        @JsonProperty("status")
        int status,

        @JsonProperty("error")
        String error,

        @JsonProperty("messages")
        List<String> messages
) {}
