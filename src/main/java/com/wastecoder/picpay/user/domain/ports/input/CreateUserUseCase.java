package com.wastecoder.picpay.user.domain.ports.input;

import com.wastecoder.picpay.user.domain.model.User;

public interface CreateUserUseCase {

    User execute(User user);

}
