package com.wastecoder.picpay.transaction;

import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.user.domain.model.User;

import java.math.BigDecimal;

public final class TransactionMother {

    public static final String DESCRIPTION_DEFAULT = "Pagamento de boleto";

    private TransactionMother() {}

    public static Transaction transactionOf(User from, User target, BigDecimal value) {
        return new Transaction(from, target, value, DESCRIPTION_DEFAULT);
    }
}
