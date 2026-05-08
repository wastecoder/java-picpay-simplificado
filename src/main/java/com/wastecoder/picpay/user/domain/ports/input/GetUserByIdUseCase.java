package com.wastecoder.picpay.user.domain.ports.input;

import com.wastecoder.picpay.user.domain.model.User;

import java.util.UUID;

public interface GetUserByIdUseCase {

    User execute(UUID userId);
}
