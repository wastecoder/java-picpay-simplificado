package com.wastecoder.picpay.transaction;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.wastecoder.picpay.transaction.adapter.controller.TransferRequestMother;
import com.wastecoder.picpay.transaction.adapter.controller.request.TransferRequest;
import com.wastecoder.picpay.transaction.adapter.controller.response.TransferResponse;
import com.wastecoder.picpay.user.UserMother;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransferIntegrationTest {

    @TestConfiguration
    static class PermissiveSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("picpay");

    @RegisterExtension
    static final WireMockExtension VALIDATOR = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static final WireMockExtension NOTIFIER = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cloud.openfeign.client.config.transfer-validation.url",
                () -> "http://localhost:" + VALIDATOR.getPort());
        registry.add("spring.cloud.openfeign.client.config.notify-user.url",
                () -> "http://localhost:" + NOTIFIER.getPort());
    }

    @Autowired TestRestTemplate http;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @LocalServerPort int port;

    @BeforeEach
    void resetState() {
        VALIDATOR.resetAll();
        NOTIFIER.resetAll();
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private String transferUrl(UUID senderId) {
        return "http://localhost:" + port + "/api/v1/users/" + senderId + "/transfer";
    }

    private long transactionRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long.class);
        return count == null ? 0L : count;
    }

    @Test
    @DisplayName("Happy path — validator allows: funds move and notification fires")
    void transfersFundsBetweenCommonUsers() {
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        userRepository.create(UserMother.commonUserWith(senderId, new BigDecimal("200.00"),
                "sender@e2e.com", "sender-doc-happy"));
        userRepository.create(UserMother.commonUserWith(targetId, BigDecimal.ZERO,
                "target@e2e.com", "target-doc-happy"));

        VALIDATOR.stubFor(post(anyUrl()).willReturn(okJson("{\"message\":\"Authorized\"}")));
        NOTIFIER.stubFor(post(anyUrl()).willReturn(ok()));

        TransferRequest body = TransferRequestMother.withTargetIdAndValue(
                targetId.toString(), new BigDecimal("50.00"));

        ResponseEntity<TransferResponse> response = http.postForEntity(
                transferUrl(senderId), body, TransferResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sentDate()).isNotNull();

        assertThat(userRepository.findById(senderId).orElseThrow().balance())
                .isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(userRepository.findById(targetId).orElseThrow().balance())
                .isEqualByComparingTo(new BigDecimal("50.00"));

        assertThat(transactionRowCount()).isEqualTo(1L);

        VALIDATOR.verify(1, postRequestedFor(anyUrl()));
        NOTIFIER.verify(1, postRequestedFor(anyUrl())
                .withRequestBody(matchingJsonPath("$.email", equalTo("target@e2e.com"))));
    }

    @Test
    @DisplayName("Validator denies — fallback returns DENIED, transaction rolls back")
    void rollsBackBalanceUpdates_whenValidatorDenies() {
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        userRepository.create(UserMother.commonUserWith(senderId, new BigDecimal("200.00"),
                "sender@e2e.com", "sender-doc-denied"));
        userRepository.create(UserMother.commonUserWith(targetId, BigDecimal.ZERO,
                "target@e2e.com", "target-doc-denied"));

        VALIDATOR.stubFor(post(anyUrl()).willReturn(forbidden()));
        NOTIFIER.stubFor(post(anyUrl()).willReturn(ok()));

        TransferRequest body = TransferRequestMother.withTargetIdAndValue(
                targetId.toString(), new BigDecimal("50.00"));

        ResponseEntity<String> response = http.postForEntity(
                transferUrl(senderId), body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(userRepository.findById(senderId).orElseThrow().balance())
                .isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(userRepository.findById(targetId).orElseThrow().balance())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(transactionRowCount()).isZero();
        NOTIFIER.verify(0, postRequestedFor(anyUrl()));
    }

    @Test
    @DisplayName("Concurrency — CHECK constraint + atomic UPDATE prevent negative balance")
    void preventsNegativeBalance_underContention() throws InterruptedException {
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        BigDecimal initialSenderBalance = new BigDecimal("99.00");
        BigDecimal transferValue = new BigDecimal("10.00");
        int threads = 10;

        userRepository.create(UserMother.commonUserWith(senderId, initialSenderBalance,
                "sender@e2e.com", "sender-doc-race"));
        userRepository.create(UserMother.commonUserWith(targetId, BigDecimal.ZERO,
                "target@e2e.com", "target-doc-race"));

        VALIDATOR.stubFor(post(anyUrl()).willReturn(okJson("{\"message\":\"Authorized\"}")));
        NOTIFIER.stubFor(post(anyUrl()).willReturn(ok()));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        TransferRequest body = TransferRequestMother.withTargetIdAndValue(
                targetId.toString(), transferValue);
        String url = transferUrl(senderId);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ResponseEntity<String> r = http.postForEntity(url, body, String.class);
                    if (r.getStatusCode().is2xxSuccessful()) {
                        successes.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    finishGate.countDown();
                }
            });
        }
        startGate.countDown();
        assertThat(finishGate.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        BigDecimal finalSender = userRepository.findById(senderId).orElseThrow().balance();
        BigDecimal finalTarget = userRepository.findById(targetId).orElseThrow().balance();

        assertThat(finalSender.signum()).isGreaterThanOrEqualTo(0);
        assertThat(finalSender.add(finalTarget))
                .isEqualByComparingTo(initialSenderBalance);
        assertThat(finalTarget.remainder(transferValue).signum()).isZero();
        assertThat(successes.get()).isBetween(1, 9);
        assertThat(transactionRowCount()).isEqualTo(successes.get());
    }
}
