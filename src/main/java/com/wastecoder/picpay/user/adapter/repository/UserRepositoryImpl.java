package com.wastecoder.picpay.user.adapter.repository;

import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.common.domain.viewmodels.SortDirection;
import com.wastecoder.picpay.user.adapter.repository.database.UserEntityDatabase;
import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;
import com.wastecoder.picpay.user.adapter.repository.mapper.UserEntityMapper;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserEntityDatabase userEntityDatabase;

    public UserRepositoryImpl(UserEntityDatabase userEntityDatabase) {
        this.userEntityDatabase = userEntityDatabase;
    }

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
    public PagedResult<User> findAll(PageQuery query) {
        List<Sort.Order> orders = query.sortOrders().stream()
                .map(o -> new Sort.Order(
                        o.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                        o.field()
                ))
                .toList();
        Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        Pageable pageable = PageRequest.of(query.page(), query.size(), sort);

        Page<UserEntity> page = userEntityDatabase.findAll(pageable);

        List<User> items = page.getContent().stream()
                .map(UserEntityMapper::fromEntityToModel)
                .toList();

        return new PagedResult<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
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
        var existing = userEntityDatabase.findByExternalId(user.id())
                .orElseThrow(UserNotFoundException::new);

        var updatedEntity = UserEntityMapper.fromModelToEntity(user);
        updatedEntity.setId(existing.getId());

        var saved = userEntityDatabase.save(updatedEntity);
        return UserEntityMapper.fromEntityToModel(saved);
    }

    @Override
    public void updateBalanceWithPlusOperation(User user, BigDecimal value) {
        userEntityDatabase.updateBalanceWithPlusOperation(user.id(), value);
    }

    @Override
    public void updateBalanceWithMinusOperation(User user, BigDecimal value) {
        userEntityDatabase.updateBalanceWithMinusOperation(user.id(), value);
    }
}
