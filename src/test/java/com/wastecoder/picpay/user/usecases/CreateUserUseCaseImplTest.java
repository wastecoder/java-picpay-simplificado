package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.wastecoder.picpay.user.UserMother.userWithBlankFullName;
import static com.wastecoder.picpay.user.UserMother.validCommonUser;
import static com.wastecoder.picpay.user.UserMother.validMerchantUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    private static ValidatorFactory validatorFactory;

    @Mock
    private UserRepository repository;

    @Mock
    private CryptoGateway cryptoGateway;

    private Validator validator;
    private CreateUserUseCase useCase;

    @BeforeAll
    static void initValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        validator = validatorFactory.getValidator();
        useCase = new CreateUserUseCaseImpl(repository, cryptoGateway, validator);
    }

    @Test
    @DisplayName("GIVEN a valid common user WHEN executing creation THEN repository persists user with encrypted password")
    void shouldPersistCommonUserWithEncryptedPassword() {
        // Given
        User input = validCommonUser();
        when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
        when(repository.findByDocument(input.document())).thenReturn(Optional.empty());
        when(cryptoGateway.encrypt(input.password())).thenReturn("encrypted-password");
        when(repository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // Then
        Assertions.assertAll(
                () -> assertThat(result.password()).isEqualTo("encrypted-password"),
                () -> assertThat(result.fullName()).isEqualTo(input.fullName()),
                () -> assertThat(result.email()).isEqualTo(input.email()),
                () -> assertThat(result.document()).isEqualTo(input.document()),
                () -> assertThat(result.type()).isEqualTo(UserType.COMMON)
        );
    }

    @Test
    @DisplayName("GIVEN a valid merchant user WHEN executing creation THEN repository persists user with encrypted password")
    void shouldPersistMerchantUserWithEncryptedPassword() {
        // Given
        User input = validMerchantUser();
        when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
        when(repository.findByDocument(input.document())).thenReturn(Optional.empty());
        when(cryptoGateway.encrypt(input.password())).thenReturn("encrypted-password");
        when(repository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // Then
        Assertions.assertAll(
                () -> assertThat(result.password()).isEqualTo("encrypted-password"),
                () -> assertThat(result.type()).isEqualTo(UserType.MERCHANT)
        );
    }

    @Test
    @DisplayName("GIVEN email already registered WHEN executing creation THEN throws EmailAlreadyRegisteredException and skips encryption and persistence")
    void shouldThrowWhenEmailAlreadyRegistered() {
        // Given
        User input = validCommonUser();
        when(repository.findByEmail(input.email())).thenReturn(Optional.of(input));

        // When / Then
        Assertions.assertThrows(EmailAlreadyRegisteredException.class, () -> useCase.execute(input));

        verify(repository, never()).findByDocument(any());
        verify(cryptoGateway, never()).encrypt(any());
        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("GIVEN document already registered WHEN executing creation THEN throws DocumentAlreadyRegisteredException and skips encryption and persistence")
    void shouldThrowWhenDocumentAlreadyRegistered() {
        // Given
        User input = validCommonUser();
        when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
        when(repository.findByDocument(input.document())).thenReturn(Optional.of(input));

        // When / Then
        Assertions.assertThrows(DocumentAlreadyRegisteredException.class, () -> useCase.execute(input));

        verify(cryptoGateway, never()).encrypt(any());
        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("GIVEN a user with bean-validation violations WHEN executing creation THEN throws ApplicationException with BAD_REQUEST status")
    void shouldThrowApplicationExceptionWhenBeanValidationFails() {
        // Given
        User input = userWithBlankFullName();

        // When / Then
        ApplicationException exception = Assertions.assertThrows(
                ApplicationException.class,
                () -> useCase.execute(input)
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repository, never()).findByEmail(any());
        verify(repository, never()).findByDocument(any());
        verify(cryptoGateway, never()).encrypt(any());
        verify(repository, never()).create(any());
    }
}
