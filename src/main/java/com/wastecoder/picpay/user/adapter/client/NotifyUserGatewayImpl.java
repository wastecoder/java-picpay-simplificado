package com.wastecoder.picpay.user.adapter.client;

import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.NotifyUserGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotifyUserGatewayImpl implements NotifyUserGateway {

    private final NotifyUserClient client;

    public NotifyUserGatewayImpl(NotifyUserClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(
            name = "notify-user",
            fallbackMethod = "notifyUserFallback"
    )
    public void notify(User user, String messageTitle, String messageBody) {
        final NotifyUserRequest message = new NotifyUserRequest(user.email(), messageTitle, messageBody);
        client.notify(message);
    }

    public void notifyUserFallback(User user, String messageTitle, String messageBody, Throwable ex) {
        log.warn(
                "Fallback triggered for notify-user. Could not notify {}. Cause: {}",
                user.email(),
                ex.toString()
        );
    }
}
