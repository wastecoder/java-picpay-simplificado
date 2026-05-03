package com.wastecoder.picpay.transaction.adapter.controller;

import com.wastecoder.picpay.transaction.adapter.controller.request.TransferRequest;

import java.math.BigDecimal;
import java.util.UUID;

public final class TransferRequestMother {

    public static final UUID       USER_ID_DEFAULT     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID       TARGET_ID_DEFAULT   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final BigDecimal VALUE_DEFAULT       = new BigDecimal("100.00");
    public static final String     DESCRIPTION_DEFAULT = "Pagamento de boleto";

    private TransferRequestMother() {}

    public static TransferRequest valid() {
        return new TransferRequest(
                TARGET_ID_DEFAULT.toString(),
                VALUE_DEFAULT,
                DESCRIPTION_DEFAULT
        );
    }

    public static TransferRequest withBlankTargetId() {
        return new TransferRequest(
                "",
                VALUE_DEFAULT,
                DESCRIPTION_DEFAULT
        );
    }

    public static TransferRequest withZeroValue() {
        return new TransferRequest(
                TARGET_ID_DEFAULT.toString(),
                BigDecimal.ZERO,
                DESCRIPTION_DEFAULT
        );
    }

    public static TransferRequest withNegativeValue() {
        return new TransferRequest(
                TARGET_ID_DEFAULT.toString(),
                new BigDecimal("-1.00"),
                DESCRIPTION_DEFAULT
        );
    }

    public static TransferRequest withBlankDescription() {
        return new TransferRequest(
                TARGET_ID_DEFAULT.toString(),
                VALUE_DEFAULT,
                ""
        );
    }

    public static TransferRequest withInvalidTargetId() {
        return new TransferRequest(
                "not-a-uuid",
                VALUE_DEFAULT,
                DESCRIPTION_DEFAULT
        );
    }
}
