package com.wastecoder.picpay.user.domain.ports.output;

import com.wastecoder.picpay.user.domain.model.TokenSession;
import com.wastecoder.picpay.user.domain.model.User;

public interface TokenGateway {

    TokenSession generate(User user);
}
