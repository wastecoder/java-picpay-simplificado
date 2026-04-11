package com.wastecoder.picpay.user.domain.ports.output;

import com.wastecoder.picpay.user.domain.model.User;

public interface NotifyUserGateway {

    void notify(User user, String messageTitle, String messageBody);

}
