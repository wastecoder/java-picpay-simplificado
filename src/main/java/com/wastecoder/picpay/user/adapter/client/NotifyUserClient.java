package com.wastecoder.picpay.user.adapter.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("notify-user")
public interface NotifyUserClient {

    @PostMapping(consumes = "application/json")
    void notify(@RequestBody NotifyUserRequest message);
}
