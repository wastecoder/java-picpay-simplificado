package com.wastecoder.picpay.user.adapter.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.TokenGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TokenGatewayImpl implements TokenGateway {

    private final Algorithm algorithm;
    private final Long jwtExpiresIn;
    private final String issuer;

    public TokenGatewayImpl(
            Algorithm algorithm,
            @Value("${security.jwt.expires-after}") String jwtExpiresIn,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.algorithm = algorithm;
        this.jwtExpiresIn = Long.valueOf(jwtExpiresIn);
        this.issuer = issuer;
    }


    public TokenResponse generate(User user) {
        String token = JWT.create()
                .withExpiresAt(new Date(System.currentTimeMillis() + (jwtExpiresIn * 1000)))
                .withSubject(user.getId().toString())
                .withIssuer(issuer)
                .sign(algorithm);

        return new TokenResponse(token, jwtExpiresIn);
    }
}
