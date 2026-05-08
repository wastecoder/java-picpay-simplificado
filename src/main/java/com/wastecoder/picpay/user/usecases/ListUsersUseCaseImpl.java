package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.user.domain.ports.input.ListUsersUseCase;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCaseImpl implements ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserSummary> execute(PageQuery query) {
        return userRepository.findAll(query)
                .map(user -> new UserSummary(user.id(), user.fullName(), user.type()));
    }
}
