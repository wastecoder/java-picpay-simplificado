package com.wastecoder.picpay.user.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;

import java.util.UUID;

public record UserSummaryResponse(

        @JsonProperty("id")
        UUID id,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("type")
        UserType type

) {

    public static UserSummaryResponse from(UserSummary summary) {
        return new UserSummaryResponse(summary.id(), summary.fullName(), summary.type());
    }
}
