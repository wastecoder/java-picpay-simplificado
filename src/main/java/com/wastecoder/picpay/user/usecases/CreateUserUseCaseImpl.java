package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository repository;
    private final CryptoGateway cryptoGateway;


    @Override
    public User execute(User user) {

        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        if (repository.findByDocument(user.getDocument()).isPresent()) {
            throw new DocumentAlreadyRegisteredException();
        }

        var userWithEncryptedPassword = user.toBuilder()
                .password(cryptoGateway.encrypt(user.getPassword()))
                .build();

        return repository.create(userWithEncryptedPassword);
    }
}
