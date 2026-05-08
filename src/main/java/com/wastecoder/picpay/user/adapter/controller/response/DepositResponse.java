package com.wastecoder.picpay.user.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepositResponse(

        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("new_balance")
        BigDecimal newBalance,

        @JsonProperty("deposited_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm", shape = JsonFormat.Shape.STRING)
        LocalDateTime depositedAt

) {}
