package com.wastecoder.picpay.user.adapter.repository.mapper;

import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;
import com.wastecoder.picpay.user.domain.model.User;

import java.util.UUID;

public class UserEntityMapper {

    private UserEntityMapper() { }

    public static UserEntity fromModelToEntity(User from) {
        return UserEntity.builder()
                .fullName(from.fullName())
                .document(from.document())
                .email(from.email())
                .type(from.type())
                .password(from.password())
                .balance(from.balance())
                .externalId(from.id() != null ? from.id() : UUID.randomUUID())
                .build();
    }

    public static User fromEntityToModel(UserEntity from) {
        return new User(
                from.getFullName(),
                from.getDocument(),
                from.getEmail(),
                from.getPassword(),
                from.getType(),
                from.getBalance(),
                from.getExternalId()
        );
    }
}
