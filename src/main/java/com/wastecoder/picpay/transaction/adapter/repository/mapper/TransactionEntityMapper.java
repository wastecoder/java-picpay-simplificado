package com.wastecoder.picpay.transaction.adapter.repository.mapper;

import com.wastecoder.picpay.transaction.adapter.repository.entity.TransactionEntity;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;

public class TransactionEntityMapper {

    private TransactionEntityMapper() { }

    public static TransactionEntity fromModelToEntity(
            Transaction from,
            UserEntity fromUserEntity,
            UserEntity targetUserEntity
    ) {
        return TransactionEntity.builder()
                .fromUser(fromUserEntity)
                .targetUser(targetUserEntity)
                .value(from.value())
                .description(from.description())
                .build();
    }
}
