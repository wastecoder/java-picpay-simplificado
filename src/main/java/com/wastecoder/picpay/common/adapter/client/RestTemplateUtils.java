package com.wastecoder.picpay.common.adapter.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class RestTemplateUtils {

    private RestTemplateUtils() {}

    public static <T, R> ResponseEntity<R> request(
            RestTemplate restTemplate,
            String endpoint,
            HttpMethod httpMethod,
            T requestBody,
            Map<String, String> headers,
            Class<R> responseType
    ) {

        HttpHeaders httpHeaders = new HttpHeaders();

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpHeaders::add);
        }

        HttpEntity<T> entity = new HttpEntity<>(requestBody, httpHeaders);

        return restTemplate.exchange(
                endpoint,
                httpMethod,
                entity,
                responseType
        );
    }
}