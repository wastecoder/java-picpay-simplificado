package com.wastecoder.picpay.common.adapter.controller.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtTokenConfiguration {

    private final String jwtSecret;

    public JwtTokenConfiguration(
            @Value("${security.jwt.secret}") String jwtSecret
    ) {
        this.jwtSecret = jwtSecret;
    }


    @Bean
    public Algorithm tokenAlgorithm() {
        return Algorithm.HMAC512(jwtSecret);
    }

    @Bean
    public JWTVerifier verifier(Algorithm algorithm) {
        return JWT
                .require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    @Value("${security.jwt.issuer}")
    private String issuer;
}
