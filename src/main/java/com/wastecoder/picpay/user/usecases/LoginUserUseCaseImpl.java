package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.IncorrectPasswordException;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.ports.input.LoginUserUseCase;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.TokenGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserCommand;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserResult;
import org.springframework.stereotype.Service;

@Service
public class LoginUserUseCaseImpl implements LoginUserUseCase {

    private final UserRepository userRepository;
    private final CryptoGateway cryptoGateway;
    private final TokenGateway tokenGateway;

    public LoginUserUseCaseImpl(UserRepository userRepository, CryptoGateway cryptoGateway, TokenGateway tokenGateway) {
        this.userRepository = userRepository;
        this.cryptoGateway = cryptoGateway;
        this.tokenGateway = tokenGateway;
    }

    @Override
    public LoginUserResult execute(LoginUserCommand command) {

        var user = userRepository.findByEmail(command.email())
                .orElseThrow(UserNotFoundException::new);

        if (!cryptoGateway.matches(command.password(), user.password())) {
            throw new IncorrectPasswordException();
        }

        var token = tokenGateway.generate(user);

        return new LoginUserResult(
                user.id(),
                token.token(),
                token.expiresIn()
        );
    }
}
