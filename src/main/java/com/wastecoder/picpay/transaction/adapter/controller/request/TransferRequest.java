package com.wastecoder.picpay.transaction.adapter.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import static com.wastecoder.picpay.common.domain.utils.UuidUtils.uuidCustomValueOf;

public record TransferRequest(

        @JsonProperty("target_id")
        @NotBlank(message = "target_id can't be blank.")
        String targetId,

        @JsonProperty("value")
        @Positive(message = "value must be positive.")
        BigDecimal value,

        @JsonProperty("description")
        @NotBlank(message = "description can't be blank.")
        @Schema(example = "message")
        String description

) {

    public TransferCommand toCommand(String fromUserId) {
        return new TransferCommand(
                uuidCustomValueOf(fromUserId, "user_id"),
                uuidCustomValueOf(targetId, "target_id"),
                value,
                description
        );
    }
}
