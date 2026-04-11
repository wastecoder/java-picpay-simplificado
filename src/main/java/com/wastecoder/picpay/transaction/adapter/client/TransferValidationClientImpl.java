package com.wastecoder.picpay.transaction.adapter.client;

import com.wastecoder.picpay.common.adapter.client.RestTemplateUtils;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.ports.output.TransferValidationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
public class TransferValidationClientImpl implements TransferValidationClient {

    private final RestTemplate restTemplate;
    private final String validateTransferEndpoint;

    public TransferValidationClientImpl(
            RestTemplate restTemplate,
            @Value("${client.transfer.validator-url}") String validateTransferEndpoint
    ) {
        this.restTemplate = restTemplate;
        this.validateTransferEndpoint = validateTransferEndpoint;
    }

    @Override
    public TransferValidationResult validate(Transaction transaction) {

        try {
            var response = RestTemplateUtils.request(
                    restTemplate,
                    validateTransferEndpoint,
                    HttpMethod.GET,
                    transaction,
                    Collections.emptyMap(),
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return TransferValidationResult.ALLOWED;
            } else {
                return TransferValidationResult.DENIED;
            }

        } catch (HttpClientErrorException e) {
            return TransferValidationResult.DENIED;
        }
    }
}
