package com.wastecoder.picpay.user.domain.ports.output;

public interface CryptoGateway {

    String encrypt(String decryptedString);

    boolean matches(String decryptedString, String encryptedString);

}
