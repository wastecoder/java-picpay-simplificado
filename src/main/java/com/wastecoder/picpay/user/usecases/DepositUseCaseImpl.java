package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.DepositUseCase;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.DepositCommand;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class DepositUseCaseImpl implements DepositUseCase {

    private final UserRepository userRepository;
    private final Clock clock;

    public DepositUseCaseImpl(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DepositResult execute(DepositCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(UserNotFoundException::new);

        userRepository.updateBalanceWithPlusOperation(user, command.value());

        User updated = userRepository.findById(command.userId())
                .orElseThrow(UserNotFoundException::new);

        return new DepositResult(
                updated.id(),
                updated.balance(),
                LocalDateTime.now(clock)
        );
    }
}
