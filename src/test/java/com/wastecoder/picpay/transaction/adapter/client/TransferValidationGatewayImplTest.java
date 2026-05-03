package com.wastecoder.picpay.transaction.adapter.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferValidationResult;
import com.wastecoder.picpay.user.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static com.wastecoder.picpay.transaction.TransactionMother.transactionOf;
import static com.wastecoder.picpay.user.UserMother.commonUserWith;
import static com.wastecoder.picpay.user.UserMother.merchantUserWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferValidationGatewayImplTest {

    private static final BigDecimal TRANSFER_VALUE = new BigDecimal("100.00");

    @Mock
    private TransferValidationClient client;

    private TransferValidationGatewayImpl gateway;
    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        gateway = new TransferValidationGatewayImpl(client);

        logger = (Logger) LoggerFactory.getLogger(TransferValidationGatewayImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    @DisplayName("GIVEN a valid transaction WHEN validating THEN client is called with same transaction and result is ALLOWED")
    void shouldCallClientAndReturnAllowed() {
        // Given
        User from = commonUserWith(UUID.randomUUID(), new BigDecimal("500.00"));
        User target = merchantUserWith(UUID.randomUUID(), BigDecimal.ZERO);
        Transaction transaction = transactionOf(from, target, TRANSFER_VALUE);

        // When
        TransferValidationResult result = gateway.validate(transaction);

        // Then
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(client).validate(captor.capture());

        Assertions.assertAll(
                () -> assertThat(captor.getValue()).isSameAs(transaction),
                () -> assertThat(result).isEqualTo(TransferValidationResult.ALLOWED)
        );
    }

    @Test
    @DisplayName("GIVEN external failure WHEN fallback triggered THEN logs warning, returns DENIED and never calls client")
    void shouldLogWarningAndReturnDeniedOnFallback() {
        // Given
        User from = commonUserWith(UUID.randomUUID(), new BigDecimal("500.00"));
        User target = merchantUserWith(UUID.randomUUID(), BigDecimal.ZERO);
        Transaction transaction = transactionOf(from, target, TRANSFER_VALUE);
        Throwable cause = new RuntimeException("circuit open");

        // When
        TransferValidationResult result = gateway.transferValidationFallback(transaction, cause);

        // Then
        verify(client, never()).validate(any());

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        Assertions.assertAll(
                () -> assertThat(result).isEqualTo(TransferValidationResult.DENIED),
                () -> assertThat(event.getLevel()).isEqualTo(Level.WARN),
                () -> assertThat(event.getFormattedMessage()).contains(from.id().toString()),
                () -> assertThat(event.getFormattedMessage()).contains(target.id().toString()),
                () -> assertThat(event.getFormattedMessage()).contains(TRANSFER_VALUE.toString()),
                () -> assertThat(event.getFormattedMessage()).contains(cause.toString()),
                () -> assertThat(event.getThrowableProxy()).isNotNull()
        );
    }
}
