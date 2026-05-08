package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.DepositUseCase;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.DepositCommand;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.wastecoder.picpay.user.UserMother.commonUserWith;
import static com.wastecoder.picpay.user.UserMother.merchantUserWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositUseCaseImplTest {

    private static final Instant    FIXED_INSTANT     = Instant.parse("2026-05-07T12:00:00Z");
    private static final UUID       USER_ID           = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BigDecimal INITIAL_BALANCE   = new BigDecimal("50.00");
    private static final BigDecimal DEPOSIT_VALUE     = new BigDecimal("100.00");
    private static final BigDecimal EXPECTED_BALANCE  = new BigDecimal("150.00");

    @Mock
    private UserRepository userRepository;

    private DepositUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        useCase = new DepositUseCaseImpl(userRepository, fixedClock);
    }

    @Test
    @DisplayName("GIVEN existing user WHEN executing deposit THEN updates balance and returns refetched balance with depositedAt")
    void shouldDepositAndReturnRefetchedBalance() {
        // Given
        User existing = commonUserWith(USER_ID, INITIAL_BALANCE);
        User afterUpdate = commonUserWith(USER_ID, EXPECTED_BALANCE);
        DepositCommand command = new DepositCommand(USER_ID, DEPOSIT_VALUE);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(afterUpdate));

        // When
        DepositResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.newBalance()).isEqualTo(EXPECTED_BALANCE);
        assertThat(result.depositedAt()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));

        InOrder inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findById(USER_ID);
        inOrder.verify(userRepository).updateBalanceWithPlusOperation(existing, DEPOSIT_VALUE);
        inOrder.verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("GIVEN merchant user WHEN executing deposit THEN deposit is allowed (no type restriction)")
    void shouldAllowMerchantDeposit() {
        // Given
        User existing = merchantUserWith(USER_ID, INITIAL_BALANCE);
        User afterUpdate = merchantUserWith(USER_ID, EXPECTED_BALANCE);
        DepositCommand command = new DepositCommand(USER_ID, DEPOSIT_VALUE);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(afterUpdate));

        // When
        DepositResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        assertThat(result.newBalance()).isEqualTo(EXPECTED_BALANCE);
        verify(userRepository).updateBalanceWithPlusOperation(existing, DEPOSIT_VALUE);
    }

    @Test
    @DisplayName("GIVEN user does not exist WHEN executing deposit THEN throws UserNotFoundException and skips balance update")
    void shouldThrowUserNotFoundWhenUserDoesNotExist() {
        // Given
        DepositCommand command = new DepositCommand(USER_ID, DEPOSIT_VALUE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        Assertions.assertThrows(UserNotFoundException.class, () -> useCase.execute(command));

        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
    }
}
