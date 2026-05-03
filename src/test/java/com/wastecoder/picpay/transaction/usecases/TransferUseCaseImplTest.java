package com.wastecoder.picpay.transaction.usecases;

import com.wastecoder.picpay.common.domain.exceptions.ApplicationException;
import com.wastecoder.picpay.transaction.domain.exceptions.InsufficientBalanceException;
import com.wastecoder.picpay.transaction.domain.exceptions.TransferNotAllowedException;
import com.wastecoder.picpay.transaction.domain.exceptions.UserCantTransferException;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.ports.input.TransferUseCase;
import com.wastecoder.picpay.transaction.domain.ports.output.TransactionRepository;
import com.wastecoder.picpay.transaction.domain.ports.output.TransferValidationGateway;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferCommand;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferResult;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferValidationResult;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.NotifyUserGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
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

import static com.wastecoder.picpay.transaction.TransactionMother.DESCRIPTION_DEFAULT;
import static com.wastecoder.picpay.transaction.TransactionMother.transactionOf;
import static com.wastecoder.picpay.user.UserMother.commonUserWith;
import static com.wastecoder.picpay.user.UserMother.merchantUserWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferUseCaseImplTest {

    private static final Instant    FIXED_INSTANT       = Instant.parse("2026-05-03T10:15:30Z");
    private static final UUID       FROM_ID             = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID       TARGET_ID           = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final BigDecimal TRANSFER_VALUE      = new BigDecimal("100.555");
    private static final BigDecimal COMFORTABLE_BALANCE = new BigDecimal("500.00");
    private static final String     EXPECTED_TITLE      = "Transferência recebida com sucesso";
    private static final String     EXPECTED_BODY       = "Você recebeu uma transferência de R$ 100.56 enviada por John Doe Smith";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferValidationGateway transferValidationGateway;

    @Mock
    private NotifyUserGateway notifyUserGateway;

    private TransferUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        useCase = new TransferUseCaseImpl(
                userRepository,
                transactionRepository,
                transferValidationGateway,
                notifyUserGateway,
                fixedClock
        );
    }

    @Test
    @DisplayName("GIVEN sender with sufficient balance and ALLOWED validation WHEN executing transfer THEN updates balances, stores transaction, notifies target and returns sentDate")
    void shouldExecuteTransferAndReturnSentDateWhenAllConditionsAreMet() {
        // Given
        User fromUser = commonUserWith(FROM_ID, COMFORTABLE_BALANCE);
        User targetUser = commonUserWith(TARGET_ID, BigDecimal.ZERO);
        Transaction expected = transactionOf(fromUser, targetUser, TRANSFER_VALUE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);

        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
        when(transferValidationGateway.validate(expected)).thenReturn(TransferValidationResult.ALLOWED);

        // When
        TransferResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        assertThat(result.sentDate()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));

        InOrder inOrder = inOrder(userRepository, transferValidationGateway, transactionRepository, notifyUserGateway);
        inOrder.verify(userRepository).findById(FROM_ID);
        inOrder.verify(userRepository).findById(TARGET_ID);
        inOrder.verify(transferValidationGateway).validate(expected);
        inOrder.verify(userRepository).updateBalanceWithMinusOperation(fromUser, TRANSFER_VALUE);
        inOrder.verify(userRepository).updateBalanceWithPlusOperation(targetUser, TRANSFER_VALUE);
        inOrder.verify(transactionRepository).storage(expected);
        inOrder.verify(notifyUserGateway).notify(targetUser, EXPECTED_TITLE, EXPECTED_BODY);
    }

    @Test
    @DisplayName("GIVEN target is a merchant WHEN executing transfer THEN persists and notifies the merchant target")
    void shouldExecuteTransferWhenTargetIsMerchant() {
        // Given
        User fromUser = commonUserWith(FROM_ID, COMFORTABLE_BALANCE);
        User targetUser = merchantUserWith(TARGET_ID, BigDecimal.ZERO);
        Transaction expected = transactionOf(fromUser, targetUser, TRANSFER_VALUE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);

        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
        when(transferValidationGateway.validate(expected)).thenReturn(TransferValidationResult.ALLOWED);

        // When
        TransferResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        assertThat(result.sentDate()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        verify(userRepository).updateBalanceWithMinusOperation(fromUser, TRANSFER_VALUE);
        verify(userRepository).updateBalanceWithPlusOperation(targetUser, TRANSFER_VALUE);
        verify(transactionRepository).storage(expected);
        verify(notifyUserGateway).notify(targetUser, EXPECTED_TITLE, EXPECTED_BODY);
    }

    @Test
    @DisplayName("GIVEN sender does not exist WHEN executing transfer THEN throws UserNotFoundException and skips all subsequent operations")
    void shouldThrowUserNotFoundWhenSenderDoesNotExist() {
        // Given
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);
        when(userRepository.findById(FROM_ID)).thenReturn(Optional.empty());

        // When / Then
        Assertions.assertThrows(UserNotFoundException.class, () -> useCase.execute(command));

        verify(userRepository, never()).findById(TARGET_ID);
        verify(transferValidationGateway, never()).validate(any());
        verify(userRepository, never()).updateBalanceWithMinusOperation(any(), any());
        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
        verify(transactionRepository, never()).storage(any());
        verify(notifyUserGateway, never()).notify(any(), any(), any());
    }

    @Test
    @DisplayName("GIVEN sender is a merchant WHEN executing transfer THEN throws UserCantTransferException and skips all subsequent operations")
    void shouldThrowUserCantTransferWhenSenderIsMerchant() {
        // Given
        User fromUser = merchantUserWith(FROM_ID, COMFORTABLE_BALANCE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);
        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));

        // When / Then
        Assertions.assertThrows(UserCantTransferException.class, () -> useCase.execute(command));

        verify(userRepository, never()).findById(TARGET_ID);
        verify(transferValidationGateway, never()).validate(any());
        verify(userRepository, never()).updateBalanceWithMinusOperation(any(), any());
        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
        verify(transactionRepository, never()).storage(any());
        verify(notifyUserGateway, never()).notify(any(), any(), any());
    }

    @Test
    @DisplayName("GIVEN sender balance is below value (boundary value - 0.01) WHEN executing transfer THEN throws InsufficientBalanceException and skips all subsequent operations")
    void shouldThrowInsufficientBalanceWhenBalanceIsLessThanValue() {
        // Given
        BigDecimal balanceJustBelow = TRANSFER_VALUE.subtract(new BigDecimal("0.01"));
        User fromUser = commonUserWith(FROM_ID, balanceJustBelow);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);
        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));

        // When / Then
        Assertions.assertThrows(InsufficientBalanceException.class, () -> useCase.execute(command));

        verify(userRepository, never()).findById(TARGET_ID);
        verify(transferValidationGateway, never()).validate(any());
        verify(userRepository, never()).updateBalanceWithMinusOperation(any(), any());
        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
        verify(transactionRepository, never()).storage(any());
        verify(notifyUserGateway, never()).notify(any(), any(), any());
    }

    @Test
    @DisplayName("GIVEN sender balance equals value (boundary that allows) WHEN executing transfer THEN persists transaction and notifies target")
    void shouldExecuteTransferWhenBalanceEqualsValue() {
        // Given
        User fromUser = commonUserWith(FROM_ID, TRANSFER_VALUE);
        User targetUser = commonUserWith(TARGET_ID, BigDecimal.ZERO);
        Transaction expected = transactionOf(fromUser, targetUser, TRANSFER_VALUE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);

        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
        when(transferValidationGateway.validate(expected)).thenReturn(TransferValidationResult.ALLOWED);

        // When
        TransferResult result = Assertions.assertDoesNotThrow(() -> useCase.execute(command));

        // Then
        assertThat(result.sentDate()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        verify(userRepository).updateBalanceWithMinusOperation(fromUser, TRANSFER_VALUE);
        verify(userRepository).updateBalanceWithPlusOperation(targetUser, TRANSFER_VALUE);
        verify(transactionRepository).storage(expected);
        verify(notifyUserGateway).notify(targetUser, EXPECTED_TITLE, EXPECTED_BODY);
    }

    @Test
    @DisplayName("GIVEN target does not exist WHEN executing transfer THEN throws UserNotFoundException with target message and skips validation, updates, storage and notification")
    void shouldThrowUserNotFoundWithTargetMessageWhenTargetDoesNotExist() {
        // Given
        User fromUser = commonUserWith(FROM_ID, COMFORTABLE_BALANCE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);
        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

        // When / Then
        ApplicationException exception = Assertions.assertThrows(
                UserNotFoundException.class,
                () -> useCase.execute(command)
        );
        assertThat(exception.getReason()).isEqualTo("Target not found.");

        verify(transferValidationGateway, never()).validate(any());
        verify(userRepository, never()).updateBalanceWithMinusOperation(any(), any());
        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
        verify(transactionRepository, never()).storage(any());
        verify(notifyUserGateway, never()).notify(any(), any(), any());
    }

    @Test
    @DisplayName("GIVEN validation gateway returns DENIED WHEN executing transfer THEN throws TransferNotAllowedException and skips updates, storage and notification")
    void shouldThrowTransferNotAllowedWhenValidationDenies() {
        // Given
        User fromUser = commonUserWith(FROM_ID, COMFORTABLE_BALANCE);
        User targetUser = commonUserWith(TARGET_ID, BigDecimal.ZERO);
        Transaction expected = transactionOf(fromUser, targetUser, TRANSFER_VALUE);
        TransferCommand command = new TransferCommand(FROM_ID, TARGET_ID, TRANSFER_VALUE, DESCRIPTION_DEFAULT);

        when(userRepository.findById(FROM_ID)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
        when(transferValidationGateway.validate(expected)).thenReturn(TransferValidationResult.DENIED);

        // When / Then
        Assertions.assertThrows(TransferNotAllowedException.class, () -> useCase.execute(command));

        verify(userRepository, never()).updateBalanceWithMinusOperation(any(), any());
        verify(userRepository, never()).updateBalanceWithPlusOperation(any(), any());
        verify(transactionRepository, never()).storage(any());
        verify(notifyUserGateway, never()).notify(any(), any(), any());
    }
}
