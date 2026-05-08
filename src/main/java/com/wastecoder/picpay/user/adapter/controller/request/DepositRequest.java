package com.wastecoder.picpay.user.adapter.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.viewmodels.DepositCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import static com.wastecoder.picpay.common.domain.utils.UuidUtils.uuidCustomValueOf;

public record DepositRequest(

        @JsonProperty("value")
        @NotNull(message = "value can't be null.")
        @Positive(message = "value must be positive.")
        BigDecimal value

) {

    public DepositCommand toCommand(String userId) {
        return new DepositCommand(
                uuidCustomValueOf(userId, "user_id"),
                value
        );
    }
}
