package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.wastecoder.picpay.user.UserObjectMother.aCommonUser;
import static com.wastecoder.picpay.user.UserObjectMother.aMerchantUser;
import static com.wastecoder.picpay.user.UserObjectMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private CryptoGateway cryptoGateway;

    private Validator validator;
    private CreateUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        useCase = new CreateUserUseCaseImpl(repository, cryptoGateway, validator);
    }

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("GIVEN a valid common user WHEN executing creation THEN repository persists user with encrypted password")
        void shouldPersistCommonUserWithEncryptedPassword() {
            // Given
            User input = aCommonUser().build();
            when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
            when(repository.findByDocument(input.document())).thenReturn(Optional.empty());
            when(cryptoGateway.encrypt(input.password())).thenReturn("encrypted-password");
            when(repository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = useCase.execute(input);

            // Then
            assertThat(result.password()).isEqualTo("encrypted-password");
            assertThat(result.fullName()).isEqualTo(input.fullName());
            assertThat(result.email()).isEqualTo(input.email());
            assertThat(result.document()).isEqualTo(input.document());
            assertThat(result.type()).isEqualTo(UserType.COMMON);
        }

        @Test
        @DisplayName("GIVEN a valid merchant user WHEN executing creation THEN repository persists user with encrypted password")
        void shouldPersistMerchantUserWithEncryptedPassword() {
            // Given
            User input = aMerchantUser().build();
            when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
            when(repository.findByDocument(input.document())).thenReturn(Optional.empty());
            when(cryptoGateway.encrypt(input.password())).thenReturn("encrypted-password");
            when(repository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = useCase.execute(input);

            // Then
            assertThat(result.password()).isEqualTo("encrypted-password");
            assertThat(result.type()).isEqualTo(UserType.MERCHANT);
        }
    }

    @Nested
    @DisplayName("Failures")
    class Failures {

        @Test
        @DisplayName("GIVEN email already registered WHEN executing creation THEN throws EmailAlreadyRegisteredException and skips encryption and persistence")
        void shouldThrowWhenEmailAlreadyRegistered() {
            // Given
            User input = aUser().build();
            when(repository.findByEmail(input.email())).thenReturn(Optional.of(input));

            // When / Then
            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(EmailAlreadyRegisteredException.class);

            verify(repository, never()).findByDocument(any());
            verify(cryptoGateway, never()).encrypt(any());
            verify(repository, never()).create(any());
        }

        @Test
        @DisplayName("GIVEN document already registered WHEN executing creation THEN throws DocumentAlreadyRegisteredException and skips encryption and persistence")
        void shouldThrowWhenDocumentAlreadyRegistered() {
            // Given
            User input = aUser().build();
            when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
            when(repository.findByDocument(input.document())).thenReturn(Optional.of(input));

            // When / Then
            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(DocumentAlreadyRegisteredException.class);

            verify(cryptoGateway, never()).encrypt(any());
            verify(repository, never()).create(any());
        }

        @Test
        @DisplayName("GIVEN a user with bean-validation violations WHEN executing creation THEN throws ApplicationException with BAD_REQUEST status")
        void shouldThrowApplicationExceptionWhenBeanValidationFails() {
            // Given
            User input = aUser().withFullName("").build();

            // When / Then
            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOfSatisfying(ApplicationException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

            verify(repository, never()).findByEmail(any());
            verify(repository, never()).findByDocument(any());
            verify(cryptoGateway, never()).encrypt(any());
            verify(repository, never()).create(any());
        }
    }

    @Nested
    @DisplayName("CallOrder")
    class CallOrder {

        @Test
        @DisplayName("GIVEN a valid user WHEN executing creation THEN dependencies are called in order: findByEmail, findByDocument, encrypt, create")
        void shouldCallDependenciesInOrder() {
            // Given
            User input = aUser().build();
            when(repository.findByEmail(input.email())).thenReturn(Optional.empty());
            when(repository.findByDocument(input.document())).thenReturn(Optional.empty());
            when(cryptoGateway.encrypt(input.password())).thenReturn("encrypted-password");
            when(repository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            useCase.execute(input);

            // Then
            InOrder inOrder = inOrder(repository, cryptoGateway);
            inOrder.verify(repository).findByEmail(input.email());
            inOrder.verify(repository).findByDocument(input.document());
            inOrder.verify(cryptoGateway).encrypt(input.password());
            inOrder.verify(repository).create(any(User.class));
            inOrder.verifyNoMoreInteractions();
        }
    }
}
