package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.user.UserMother;
import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;

public final class CreateUserRequestMother {

    public static final String FULL_NAME_DEFAULT = UserMother.FULL_NAME_DEFAULT;
    public static final String DOCUMENT_DEFAULT  = UserMother.DOCUMENT_DEFAULT;
    public static final String EMAIL_DEFAULT     = UserMother.EMAIL_DEFAULT;
    public static final String PASSWORD_DEFAULT  = UserMother.PASSWORD_DEFAULT;
    public static final String TYPE_COMMON       = "COMMON";
    public static final String TYPE_MERCHANT     = "MERCHANT";

    private CreateUserRequestMother() {}

    public static CreateUserRequest valid() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                TYPE_COMMON
        );
    }

    public static CreateUserRequest validMerchant() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                TYPE_MERCHANT
        );
    }

    public static CreateUserRequest withBlankFullName() {
        return new CreateUserRequest(
                "",
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                TYPE_COMMON
        );
    }

    public static CreateUserRequest withBlankDocument() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                "",
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                TYPE_COMMON
        );
    }

    public static CreateUserRequest withBlankEmail() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                "",
                PASSWORD_DEFAULT,
                TYPE_COMMON
        );
    }

    public static CreateUserRequest withBlankPassword() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                "",
                TYPE_COMMON
        );
    }

    public static CreateUserRequest withBlankType() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                ""
        );
    }

    public static CreateUserRequest withInvalidType() {
        return new CreateUserRequest(
                FULL_NAME_DEFAULT,
                DOCUMENT_DEFAULT,
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT,
                "INVALID"
        );
    }
}
