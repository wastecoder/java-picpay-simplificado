package com.wastecoder.picpay.user;

import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.model.User;

import java.math.BigDecimal;
import java.util.UUID;

public final class UserMother {

    public static final String     FULL_NAME_DEFAULT = "John Doe Smith";
    public static final String     DOCUMENT_DEFAULT  = "123.456.789-00";
    public static final String     EMAIL_DEFAULT     = "john.doe@example.com";
    public static final String     PASSWORD_DEFAULT  = "secret-password";
    public static final UserType   TYPE_DEFAULT      = UserType.COMMON;
    public static final BigDecimal BALANCE_DEFAULT   = BigDecimal.ZERO;
    public static final UUID       ID_DEFAULT        = null;

    private UserMother() {}

    public static User validCommonUser() {
        return new User(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.COMMON,
                BALANCE_DEFAULT,
                ID_DEFAULT
        );
    }

    public static User validCommonUserWithId(UUID id) {
        return new User(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.COMMON,
                BALANCE_DEFAULT,
                id
        );
    }

    public static User commonUserWith(UUID id, BigDecimal balance) {
        return new User(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.COMMON,
                balance,
                id
        );
    }

    public static User merchantUserWith(UUID id, BigDecimal balance) {
        return new User(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.MERCHANT,
                balance,
                id
        );
    }

    public static User validMerchantUser() {
        return new User(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.MERCHANT,
                BALANCE_DEFAULT,
                ID_DEFAULT
        );
    }

    public static User userWithBlankFullName() {
        return new User(
                "",
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                UserType.COMMON,
                BALANCE_DEFAULT,
                ID_DEFAULT
        );
    }
}
