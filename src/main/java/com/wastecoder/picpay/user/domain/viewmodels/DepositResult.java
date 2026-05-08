package com.wastecoder.picpay.user.domain.viewmodels;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepositResult(
        UUID userId,
        BigDecimal newBalance,
        LocalDateTime depositedAt
) {}
