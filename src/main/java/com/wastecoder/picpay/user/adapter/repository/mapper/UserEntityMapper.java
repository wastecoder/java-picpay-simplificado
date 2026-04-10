package com.wastecoder.picpay.user.adapter.repository.mapper;

import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;
import com.wastecoder.picpay.user.domain.model.User;

import java.util.UUID;

public class UserEntityMapper {

    private UserEntityMapper() { }

    public static UserEntity fromModelToEntity(User from) {
        return UserEntity.builder()
                .fullName(from.getFullName())
                .document(from.getDocument())
                .email(from.getEmail())
                .type(from.getType())
                .password(from.getPassword())
                .balance(from.getBalance())
                .externalId(from.getId() != null ? from.getId() : UUID.randomUUID())
                .build();
    }

    public static User fromEntityToModel(UserEntity from) {
        return User.builder()
                .fullName(from.getFullName())
                .document(from.getDocument())
                .email(from.getEmail())
                .type(from.getType())
                .password(from.getPassword())
                .balance(from.getBalance())
                .id(from.getExternalId())
                .build();
    }
}