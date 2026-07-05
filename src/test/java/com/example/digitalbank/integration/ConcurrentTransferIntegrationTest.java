package com.example.digitalbank.integration;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.repository.AccountRepository;
import com.example.digitalbank.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConcurrentTransferIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
    }

    @Autowired
    TransferService transferService;

    @Autowired
    AccountRepository accountRepository;

    @Test
    void concurrentTransfersKeepTotalBalanceConsistent() throws InterruptedException {
        Account a = new Account();
        a.setFirstName("Concurrent");
        a.setLastName("A");
        a.setBalance(new BigDecimal("1000.00"));
        a.setRole("USER");

        Account b = new Account();
        b.setFirstName("Concurrent");
        b.setLastName("B");
        b.setBalance(new BigDecimal("1000.00"));
        b.setRole("USER");

        accountRepository.save(a);
        accountRepository.save(b);

        BigDecimal totalBefore = a.getBalance().add(b.getBalance());
        int threads = 50;
        CountDownLatch latch = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                boolean aToB = i % 2 == 0;
                pool.submit(() -> {
                    try {
                        UUID from = aToB ? a.getId() : b.getId();
                        UUID to = aToB ? b.getId() : a.getId();
                        transferService.transfer(from, to, new BigDecimal("10.00"));
                    } catch (Exception ignored) {
                        // insufficient funds, etc are expected under contention, only the final totals matter here
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completedInTime = latch.await(60, TimeUnit.SECONDS);
            assertThat(completedInTime).as("all 50 concurrent transfers should finish within 60s").isTrue();
        }

        BigDecimal totalAfter = accountRepository.findById(a.getId()).orElseThrow().getBalance()
                .add(accountRepository.findById(b.getId()).orElseThrow().getBalance());

        assertThat(totalAfter).isEqualByComparingTo(totalBefore);
        assertThat(accountRepository.findById(a.getId()).orElseThrow().getBalance())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(accountRepository.findById(b.getId()).orElseThrow().getBalance())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
}