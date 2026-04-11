package com.wastecoder.picpay.user.adapter.client;

import com.wastecoder.picpay.common.adapter.client.NotifyMessage;
import com.wastecoder.picpay.common.adapter.client.RestTemplateUtils;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.NotifyUserGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
public class NotifyUserGatewayImpl implements NotifyUserGateway {

    private final RestTemplate restTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final String notifyUserEndpoint;

    public NotifyUserGatewayImpl(
            RestTemplate restTemplate,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            @Value("${client.notify.sender-url}") String notifyUserEndpoint
    ) {
        this.restTemplate = restTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.notifyUserEndpoint = notifyUserEndpoint;
    }

    @Override
    public void notify(User user, String messageTitle, String messageBody) {

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitbreaker");

        circuitBreaker.run(() -> {
            RestTemplateUtils.request(
                    restTemplate,
                    notifyUserEndpoint,
                    HttpMethod.POST,
                    new NotifyMessage(
                            user.getEmail(),
                            messageTitle,
                            messageBody
                    ),
                    Collections.emptyMap(),
                    Void.class
            );
            return null;
        });
    }
}
