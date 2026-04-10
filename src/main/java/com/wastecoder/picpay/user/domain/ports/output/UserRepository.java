package com.wastecoder.picpay.user.domain.ports.output;

import com.wastecoder.picpay.user.domain.model.User;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDocument(String document);

    User create(User user);
    User update(User user);

    void updateBalanceWithPlusOperation(User user, BigDecimal value);
    void updateBalanceWithMinusOperation(User user, BigDecimal value);
}