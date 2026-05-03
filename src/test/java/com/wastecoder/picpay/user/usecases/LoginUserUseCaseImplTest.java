package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.IncorrectPasswordException;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.TokenSession;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.LoginUserUseCase;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.TokenGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserCommand;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.wastecoder.picpay.user.UserMother.EMAIL_DEFAULT;
import static com.wastecoder.picpay.user.UserMother.PASSWORD_DEFAULT;
import static com.wastecoder.picpay.user.UserMother.validCommonUserWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseImplTest {

    private static final UUID   USER_ID_DEFAULT      = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ACCESS_TOKEN_DEFAULT = "access-token-abc";
    private static final long   EXPIRES_IN_DEFAULT   = 3600L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CryptoGateway cryptoGateway;

    @Mock
    private TokenGateway tokenGateway;

    private LoginUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUserUseCaseImpl(userRepository, cryptoGateway, tokenGateway);
    }

    @Test
    @DisplayName("GIVEN valid credentials WHEN executing login THEN returns LoginUserResult with user id, access token and expiresIn")
    void shouldReturnLoginUserResultWhenCredentialsAreValid() {
        // Given
        User user = validCommonUserWithId(USER_ID_DEFAULT);
        LoginUserCommand command = new LoginUserCommand(EMAIL_DEFAULT, PASSWORD_DEFAULT);
        when(userRepository.findByEmail(EMAIL_DEFAULT)).thenReturn(Optional.of(user));
        when(cryptoGateway.matches(PASSWORD_DEFAULT, user.password())).thenReturn(true);
        when(tokenGateway.generate(user)).thenReturn(new TokenSession(ACCESS_TOKEN_DEFAULT, EXPIRES_IN_DEFAULT));

        // When
        LoginUserResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        Assertions.assertAll(
                () -> assertThat(result.userId()).isEqualTo(USER_ID_DEFAULT),
                () -> assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN_DEFAULT),
                () -> assertThat(result.expiresIn()).isEqualTo(EXPIRES_IN_DEFAULT)
        );

        InOrder inOrder = inOrder(userRepository, cryptoGateway, tokenGateway);
        inOrder.verify(userRepository).findByEmail(EMAIL_DEFAULT);
        inOrder.verify(cryptoGateway).matches(PASSWORD_DEFAULT, user.password());
        inOrder.verify(tokenGateway).generate(user);
    }

    @Test
    @DisplayName("GIVEN email not registered WHEN executing login THEN throws UserNotFoundException and skips password check and token generation")
    void shouldThrowUserNotFoundWhenEmailDoesNotExist() {
        // Given
        LoginUserCommand command = new LoginUserCommand(EMAIL_DEFAULT, PASSWORD_DEFAULT);
        when(userRepository.findByEmail(EMAIL_DEFAULT)).thenReturn(Optional.empty());

        // When / Then
        Assertions.assertThrows(UserNotFoundException.class, () -> useCase.execute(command));

        verify(cryptoGateway, never()).matches(any(), any());
        verify(tokenGateway, never()).generate(any());
    }

    @Test
    @DisplayName("GIVEN incorrect password WHEN executing login THEN throws IncorrectPasswordException and skips token generation")
    void shouldThrowIncorrectPasswordWhenPasswordDoesNotMatch() {
        // Given
        User user = validCommonUserWithId(USER_ID_DEFAULT);
        LoginUserCommand command = new LoginUserCommand(EMAIL_DEFAULT, PASSWORD_DEFAULT);
        when(userRepository.findByEmail(EMAIL_DEFAULT)).thenReturn(Optional.of(user));
        when(cryptoGateway.matches(PASSWORD_DEFAULT, user.password())).thenReturn(false);

        // When / Then
        Assertions.assertThrows(IncorrectPasswordException.class, () -> useCase.execute(command));

        verify(tokenGateway, never()).generate(any());
    }
}
