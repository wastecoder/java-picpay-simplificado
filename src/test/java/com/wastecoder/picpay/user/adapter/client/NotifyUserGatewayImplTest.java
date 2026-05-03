package com.wastecoder.picpay.user.adapter.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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

import static com.wastecoder.picpay.user.NotificationMother.BODY_DEFAULT;
import static com.wastecoder.picpay.user.NotificationMother.TITLE_DEFAULT;
import static com.wastecoder.picpay.user.UserMother.validCommonUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyUserGatewayImplTest {

    @Mock
    private NotifyUserClient client;

    private NotifyUserGatewayImpl gateway;
    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        gateway = new NotifyUserGatewayImpl(client);

        logger = (Logger) LoggerFactory.getLogger(NotifyUserGatewayImpl.class);
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
    @DisplayName("GIVEN a valid user WHEN notifying THEN client receives request with email title and body")
    void shouldSendRequestWithCorrectPayload() {
        // Given
        User input = validCommonUser();

        // When
        Assertions.assertDoesNotThrow(() -> gateway.notify(input, TITLE_DEFAULT, BODY_DEFAULT));

        // Then
        ArgumentCaptor<NotifyUserRequest> captor = ArgumentCaptor.forClass(NotifyUserRequest.class);
        verify(client).notify(captor.capture());

        NotifyUserRequest sent = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(sent.email()).isEqualTo(input.email()),
                () -> assertThat(sent.messageTitle()).isEqualTo(TITLE_DEFAULT),
                () -> assertThat(sent.messageBody()).isEqualTo(BODY_DEFAULT)
        );
    }

    @Test
    @DisplayName("GIVEN external failure WHEN fallback triggered THEN logs warning and never calls client")
    void shouldLogWarningAndSkipClientOnFallback() {
        // Given
        User input = validCommonUser();
        Throwable cause = new RuntimeException("circuit open");

        // When
        Assertions.assertDoesNotThrow(() ->
                gateway.notifyUserFallback(input, TITLE_DEFAULT, BODY_DEFAULT, cause)
        );

        // Then
        verify(client, never()).notify(any());

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        Assertions.assertAll(
                () -> assertThat(event.getLevel()).isEqualTo(Level.WARN),
                () -> assertThat(event.getFormattedMessage()).contains(input.email()),
                () -> assertThat(event.getFormattedMessage()).contains(cause.toString())
        );
    }
}
