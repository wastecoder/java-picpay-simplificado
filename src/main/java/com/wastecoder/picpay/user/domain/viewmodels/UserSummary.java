package com.wastecoder.picpay.user.domain.viewmodels;

import com.wastecoder.picpay.user.domain.enums.UserType;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String fullName,
        UserType type
) {}
