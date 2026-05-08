package com.wastecoder.picpay.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.model.User;
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
class ListUsersIntegrationTest {

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

    private String listUrl(String query) {
        return "http://localhost:" + port + "/api/v1/users" + query;
    }

    @Test
    @DisplayName("Returns first page sorted asc, only id/full_name/type per item, with correct pagination metadata")
    void returnsFirstPageSortedAndExposesOnlyAllowedFields() {
        seedUser("Alice Silva",   "alice@e2e.com",   "alice-doc");
        seedUser("Bob Souza",     "bob@e2e.com",     "bob-doc");
        seedUser("Carol Pereira", "carol@e2e.com",   "carol-doc");

        ResponseEntity<JsonNode> response = http.getForEntity(
                listUrl("?size=2&sort=fullName,asc"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("total_elements").asLong()).isEqualTo(3L);
        assertThat(body.get("total_pages").asInt()).isEqualTo(2);
        assertThat(body.get("last").asBoolean()).isFalse();

        JsonNode content = body.get("content");
        assertThat(content.size()).isEqualTo(2);
        assertThat(content.get(0).get("full_name").asText()).isEqualTo("Alice Silva");
        assertThat(content.get(1).get("full_name").asText()).isEqualTo("Bob Souza");

        JsonNode firstItem = content.get(0);
        assertThat(firstItem.has("id")).isTrue();
        assertThat(firstItem.has("full_name")).isTrue();
        assertThat(firstItem.has("type")).isTrue();
        assertThat(firstItem.has("email")).isFalse();
        assertThat(firstItem.has("document")).isFalse();
        assertThat(firstItem.has("balance")).isFalse();
        assertThat(firstItem.has("password")).isFalse();
    }

    @Test
    @DisplayName("Returns last page with last=true and remaining items")
    void returnsLastPageMarkedAsLast() {
        seedUser("Alice Silva",   "alice@e2e.com",   "alice-doc");
        seedUser("Bob Souza",     "bob@e2e.com",     "bob-doc");
        seedUser("Carol Pereira", "carol@e2e.com",   "carol-doc");

        ResponseEntity<JsonNode> response = http.getForEntity(
                listUrl("?page=1&size=2&sort=fullName,asc"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("page").asInt()).isEqualTo(1);
        assertThat(body.get("last").asBoolean()).isTrue();
        assertThat(body.get("content").size()).isEqualTo(1);
        assertThat(body.get("content").get(0).get("full_name").asText()).isEqualTo("Carol Pereira");
    }

    private void seedUser(String fullName, String email, String document) {
        userRepository.create(new User(
                fullName,
                document,
                email,
                UserMother.PASSWORD_DEFAULT,
                UserType.COMMON,
                BigDecimal.ZERO,
                UUID.randomUUID()
        ));
    }
}
