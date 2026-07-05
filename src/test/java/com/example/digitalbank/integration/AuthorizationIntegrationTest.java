package com.example.digitalbank.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthorizationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Value("${local.server.port}")
    private int port;

    private final RestTemplate rest = new RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void cannotTransferFromSomeoneElsesAccount() {
        Map<?, ?> accountA = register("alice-it", "password123");
        Map<?, ?> accountB = register("bruno-it", "password123");
        String tokenA = login("alice-it", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "fromAccountId", Objects.requireNonNull(accountB.get("id")),
                "toAccountId", Objects.requireNonNull(accountA.get("id")),
                "amount", 10.00
        );

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                rest.postForEntity(url("/api/transfers"), new HttpEntity<>(body, headers), String.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonAdminCannotReachAdminEndpoints() {
        register("carla-it", "password123");
        String token = login("carla-it", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                rest.exchange(url("/api/admin/accounts"), HttpMethod.GET, new HttpEntity<>(headers), String.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<?, ?> register(String username, String password) {
        Map<String, Object> body = Map.of(
                "firstName", username, "lastName", "Test",
                "username", username, "password", password,
                "initialBalance", 1000.00
        );
        return rest.postForObject(url("/api/accounts"), body, Map.class);
    }

    private String login(String username, String password) {
        Map<?, ?> response = rest.postForObject(
                url("/api/auth/login"), Map.of("username", username, "password", password), Map.class);
        return (String) Objects.requireNonNull(response).get("accessToken");
    }
}