package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository repository;
    private final CryptoGateway cryptoGateway;
    private final Validator validator;

    public CreateUserUseCaseImpl(
            UserRepository repository,
            CryptoGateway cryptoGateway,
            Validator validator
    ) {
        this.repository = repository;
        this.cryptoGateway = cryptoGateway;
        this.validator = validator;
    }

    @Override
    public User execute(User user) {
        ensureValid(user);

        if (repository.findByEmail(user.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        if (repository.findByDocument(user.document()).isPresent()) {
            throw new DocumentAlreadyRegisteredException();
        }

        var encrypted = user.withPassword(cryptoGateway.encrypt(user.password()));
        return repository.create(encrypted);
    }

    private void ensureValid(User user) {
        var violations = validator.validate(user);
        if (violations.isEmpty()) return;

        var message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        throw new ApplicationException(HttpStatus.BAD_REQUEST, message);
    }
}
