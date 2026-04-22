package com.wastecoder.picpay.transaction.adapter.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferValidationResponse(

        @JsonProperty("message")
        String message

) {}
