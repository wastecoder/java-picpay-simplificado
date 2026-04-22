package com.wastecoder.picpay.transaction.domain.viewmodels;

import java.time.LocalDateTime;

public record TransferResult(
        LocalDateTime sentDate
) {}
