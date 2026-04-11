package com.wastecoder.picpay.transaction.domain.ports.input;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransferUseCase {

    TransferResult execute(TransferCommand command);

    record TransferCommand(
            UUID fromUserId,
            UUID targetUserId,
            BigDecimal value,
            String description
    ) {}

    record TransferResult(
            LocalDateTime sentDate
    ) {}
}
