package com.wastecoder.picpay.user.domain.ports.input;

import java.util.UUID;

public interface LoginUserUseCase {

    LoginUserResult execute(LoginUserCommand command);


    record LoginUserCommand(String email, String password) {}

    record LoginUserResult(UUID userId, String accessToken, long expiresIn) {}
}
