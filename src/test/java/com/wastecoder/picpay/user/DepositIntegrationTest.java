package com.wastecoder.picpay.user;

import com.wastecoder.picpay.user.adapter.controller.response.DepositResponse;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DepositIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("picpay");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @LocalServerPort int port;

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private String depositUrl(UUID userId) {
        return "http://localhost:" + port + "/api/v1/users/" + userId + "/deposit";
    }

    @Test
    @DisplayName("Deposit returns the fresh updated balance and persists it (regression for stale persistence-context bug)")
    void depositReturnsFreshBalanceAndPersists() {
        UUID userId = UUID.randomUUID();
        userRepository.create(UserMother.commonUserWith(userId, BigDecimal.ZERO,
                "deposit@e2e.com", "deposit-doc"));

        ResponseEntity<DepositResponse> first = http.postForEntity(
                depositUrl(userId), new DepositPayload(new BigDecimal("150.00")), DepositResponse.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().userId()).isEqualTo(userId);
        assertThat(first.getBody().newBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(first.getBody().depositedAt()).isNotNull();

        assertThat(userRepository.findById(userId).orElseThrow().balance())
                .isEqualByComparingTo(new BigDecimal("150.00"));

        ResponseEntity<DepositResponse> second = http.postForEntity(
                depositUrl(userId), new DepositPayload(new BigDecimal("100.00")), DepositResponse.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().newBalance()).isEqualByComparingTo(new BigDecimal("250.00"));

        assertThat(userRepository.findById(userId).orElseThrow().balance())
                .isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Deposit returns 404 when user does not exist")
    void depositReturnsNotFoundForUnknownUser() {
        UUID unknownId = UUID.randomUUID();

        ResponseEntity<String> response = http.postForEntity(
                depositUrl(unknownId), new DepositPayload(new BigDecimal("10.00")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private record DepositPayload(BigDecimal value) {}
}
