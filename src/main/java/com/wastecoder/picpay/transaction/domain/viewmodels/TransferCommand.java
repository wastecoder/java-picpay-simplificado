package com.wastecoder.picpay.transaction.domain.viewmodels;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(
        UUID fromUserId,
        UUID targetUserId,
        BigDecimal value,
        String description
) {}
