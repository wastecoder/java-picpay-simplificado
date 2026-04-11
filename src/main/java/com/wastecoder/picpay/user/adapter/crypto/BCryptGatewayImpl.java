package com.wastecoder.picpay.user.adapter.crypto;

import com.wastecoder.picpay.user.domain.ports.output.CryptoGateway;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptGatewayImpl implements CryptoGateway {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();


    @Override
    public String encrypt(String decryptedString) {
        return delegate.encode(decryptedString);
    }

    @Override
    public boolean matches(String decryptedString, String encryptedString) {
        return delegate.matches(decryptedString, encryptedString);
    }
}
