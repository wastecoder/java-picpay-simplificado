package com.wastecoder.picpay.transaction.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TransferResponse(

        @JsonProperty("sent_date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm", shape = JsonFormat.Shape.STRING)
        LocalDateTime sentDate,

        @JsonProperty("message")
        String message

) {}
