package com.wastecoder.picpay.user.domain.viewmodels;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositCommand(
        UUID userId,
        BigDecimal value
) {}
