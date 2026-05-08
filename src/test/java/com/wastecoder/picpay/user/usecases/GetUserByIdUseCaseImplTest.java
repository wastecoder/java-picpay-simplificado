package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.GetUserByIdUseCase;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.wastecoder.picpay.user.UserMother.commonUserWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByIdUseCaseImplTest {

    private static final UUID       USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BigDecimal BALANCE = new BigDecimal("75.00");

    @Mock
    private UserRepository userRepository;

    private GetUserByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserByIdUseCaseImpl(userRepository);
    }

    @Test
    @DisplayName("GIVEN existing user WHEN executing get-by-id THEN returns the user from repository")
    void shouldReturnUserWhenFound() {
        // Given
        User existing = commonUserWith(USER_ID, BALANCE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        // When
        User result = Assertions.assertDoesNotThrow(() -> useCase.execute(USER_ID));

        // Then
        assertThat(result).isSameAs(existing);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("GIVEN user does not exist WHEN executing get-by-id THEN throws UserNotFoundException")
    void shouldThrowUserNotFoundWhenUserDoesNotExist() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        Assertions.assertThrows(UserNotFoundException.class, () -> useCase.execute(USER_ID));
        verify(userRepository).findById(USER_ID);
    }
}
