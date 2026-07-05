package com.example.digitalbank.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "a-very-long-test-secret-key-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);
    }

    @Test
    void roundTripsAccountId() {
        UUID id = UUID.randomUUID();
        String token = jwtService.generateToken(id, "alice");
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractAccountId(token)).isEqualTo(id);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(UUID.randomUUID(), "alice");
        assertThat(jwtService.isValid(token + "x")).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }
}