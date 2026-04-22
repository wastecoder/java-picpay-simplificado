package com.wastecoder.picpay.transaction.adapter.client;

import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.ports.output.TransferValidationGateway;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferValidationResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransferValidationGatewayImpl implements TransferValidationGateway {

    private final TransferValidationClient client;

    public TransferValidationGatewayImpl(TransferValidationClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(
            name = "transfer-validation",
            fallbackMethod = "transferValidationFallback"
    )
    public TransferValidationResult validate(Transaction transaction) {
        client.validate(transaction);
        return TransferValidationResult.ALLOWED;
    }

    public TransferValidationResult transferValidationFallback(
            Transaction transaction,
            Throwable ex
    ) {
        log.warn(
                "Transfer validation fallback triggered | fromUserId={} | toUserId={} | amount={} | cause={}",
                transaction.from().id(),
                transaction.target().id(),
                transaction.value(),
                ex.toString(),
                ex
        );

        return TransferValidationResult.DENIED;
    }
}
