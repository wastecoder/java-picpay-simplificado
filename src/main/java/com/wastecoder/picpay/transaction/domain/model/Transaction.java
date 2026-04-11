package com.wastecoder.picpay.transaction.domain.model;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.model.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
public class Transaction {

    private final User from;
    private final User target;
    private final BigDecimal value;
    private final String description;

    public Transaction(User from,
                       User target,
                       BigDecimal value,
                       String description) {

        this.from = from;
        this.target = target;
        this.value = value;
        this.description = description;

        validate();
    }

    private void validate() {

        if (from == null) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Sender (from) must not be null"
            );
        }

        if (target == null) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Target must not be null"
            );
        }

        if (value == null) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Value must not be null"
            );
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Value must be positive"
            );
        }

        if (description == null || description.isBlank()) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "Description must not be blank"
            );
        }
    }
}
