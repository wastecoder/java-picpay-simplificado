package com.wastecoder.picpay.user.domain.enums;

import java.util.Optional;

public enum UserType {
    COMMON,
    MERCHANT;


    public static Optional<UserType> findByName(String name) {
        for (UserType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}