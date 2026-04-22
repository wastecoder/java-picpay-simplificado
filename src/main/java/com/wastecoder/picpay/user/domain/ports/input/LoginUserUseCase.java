package com.wastecoder.picpay.user.domain.ports.input;

import com.wastecoder.picpay.user.domain.viewmodels.LoginUserCommand;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserResult;

public interface LoginUserUseCase {

    LoginUserResult execute(LoginUserCommand command);
}
