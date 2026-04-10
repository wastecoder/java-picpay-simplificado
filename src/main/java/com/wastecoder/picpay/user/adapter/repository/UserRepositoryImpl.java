package com.wastecoder.picpay.user.adapter.repository;

import com.wastecoder.picpay.user.adapter.repository.database.UserEntityDatabase;
import com.wastecoder.picpay.user.adapter.repository.mapper.UserEntityMapper;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserEntityDatabase userEntityDatabase;


    @Override
    public Optional<User> findById(UUID id) {
        return userEntityDatabase.findByExternalId(id)
                .map(UserEntityMapper::fromEntityToModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userEntityDatabase.findByEmail(email)
                .map(UserEntityMapper::fromEntityToModel);
    }

    @Override
    public Optional<User> findByDocument(String document) {
        return userEntityDatabase.findByDocument(document)
                .map(UserEntityMapper::fromEntityToModel);
    }

    @Override
    public User create(User user) {
        var saved = userEntityDatabase.save(
                UserEntityMapper.fromModelToEntity(user)
        );
        return UserEntityMapper.fromEntityToModel(saved);
    }

    @Override
    public User update(User user) {
        var existing = userEntityDatabase.findByExternalId(user.getId())
                .orElseThrow(UserNotFoundException::new);

        var updatedEntity = UserEntityMapper.fromModelToEntity(user);
        updatedEntity.setId(existing.getId());

        var saved = userEntityDatabase.save(updatedEntity);
        return UserEntityMapper.fromEntityToModel(saved);
    }

    @Override
    public void updateBalanceWithPlusOperation(User user, BigDecimal value) {
        userEntityDatabase.updateBalanceWithPlusOperation(user.getId(), value);
    }

    @Override
    public void updateBalanceWithMinusOperation(User user, BigDecimal value) {
        userEntityDatabase.updateBalanceWithMinusOperation(user.getId(), value);
    }
}
