package com.wastecoder.picpay.transaction.adapter.repository.database;

import com.wastecoder.picpay.transaction.adapter.repository.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionEntityDatabase extends JpaRepository<TransactionEntity, Long> {
}
