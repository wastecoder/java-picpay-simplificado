package com.wastecoder.picpay.transaction.adapter.repository;

import com.wastecoder.picpay.transaction.adapter.repository.database.TransactionEntityDatabase;
import com.wastecoder.picpay.transaction.adapter.repository.mapper.TransactionEntityMapper;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.ports.output.TransactionRepository;
import com.wastecoder.picpay.user.adapter.repository.database.UserEntityDatabase;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    private final TransactionEntityDatabase transactionEntityDatabase;
    private final UserEntityDatabase userEntityDatabase;

    public TransactionRepositoryImpl(
            TransactionEntityDatabase transactionEntityDatabase,
            UserEntityDatabase userEntityDatabase
    ) {
        this.transactionEntityDatabase = transactionEntityDatabase;
        this.userEntityDatabase = userEntityDatabase;
    }

    @Override
    public void storage(Transaction transaction) {
        var fromUserEntity = userEntityDatabase.findByExternalId(transaction.from().id())
                .orElseThrow(UserNotFoundException::new);
        var targetUserEntity = userEntityDatabase.findByExternalId(transaction.target().id())
                .orElseThrow(UserNotFoundException::new);

        transactionEntityDatabase.save(
                TransactionEntityMapper.fromModelToEntity(transaction, fromUserEntity, targetUserEntity)
        );
    }
}
