package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.user.UserMother;
import com.wastecoder.picpay.user.adapter.controller.request.LoginUserRequest;

public final class LoginUserRequestMother {

    public static final String EMAIL_DEFAULT    = UserMother.EMAIL_DEFAULT;
    public static final String PASSWORD_DEFAULT = UserMother.PASSWORD_DEFAULT;

    private LoginUserRequestMother() {}

    public static LoginUserRequest valid() {
        return new LoginUserRequest(
                EMAIL_DEFAULT,
                PASSWORD_DEFAULT
        );
    }

    public static LoginUserRequest withBlankEmail() {
        return new LoginUserRequest(
                "",
                PASSWORD_DEFAULT
        );
    }

    public static LoginUserRequest withBlankPassword() {
        return new LoginUserRequest(
                EMAIL_DEFAULT,
                ""
        );
    }

    public static LoginUserRequest withBothBlank() {
        return new LoginUserRequest(
                "",
                ""
        );
    }
}
