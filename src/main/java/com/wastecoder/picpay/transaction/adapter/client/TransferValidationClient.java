package com.wastecoder.picpay.transaction.adapter.client;

import com.wastecoder.picpay.transaction.domain.model.Transaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("transfer-validation")
public interface TransferValidationClient {

    @PostMapping(consumes = "application/json")
    TransferValidationResponse validate(@RequestBody Transaction transaction);
}
