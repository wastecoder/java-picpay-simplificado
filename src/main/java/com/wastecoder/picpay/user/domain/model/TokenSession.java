package com.wastecoder.picpay.user.domain.model;

public record TokenSession(
        String token,
        long expiresIn
) {}
