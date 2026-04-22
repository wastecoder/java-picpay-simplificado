package com.wastecoder.picpay.user.domain.viewmodels;

import java.util.UUID;

public record LoginUserResult(
        UUID userId,
        String accessToken,
        long expiresIn
) {}
