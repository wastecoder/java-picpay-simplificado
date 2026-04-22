package com.wastecoder.picpay.user.domain.viewmodels;

public record LoginUserCommand(
        String email,
        String password
) {}
