package com.wastecoder.picpay.user.adapter.repository.database;

import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEntityDatabase extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDocument(String document);

    Optional<UserEntity> findByExternalId(UUID externalId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.balance = u.balance + :value WHERE u.externalId = :externalId")
    void updateBalanceWithPlusOperation(UUID externalId, BigDecimal value);

    @Modifying
    @Query("UPDATE UserEntity u SET u.balance = u.balance - :value WHERE u.externalId = :externalId")
    void updateBalanceWithMinusOperation(UUID externalId, BigDecimal value);
}
