package com.wastecoder.picpay.user.domain.enums;

public enum UserType {
    COMMON,
    MERCHANT;

    public static UserType findByName(String name) {
        for (UserType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}