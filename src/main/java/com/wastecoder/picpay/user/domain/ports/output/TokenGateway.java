package com.wastecoder.picpay.user.domain.ports.output;

import com.wastecoder.picpay.user.domain.model.User;

public interface TokenGateway {

    TokenResponse generate(User user);


    record TokenResponse(String token, long expiresIn) {}
}
