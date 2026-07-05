package com.example.digitalbank.config;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.repository.AccountRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdminAccountSeeder implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (accountRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Account admin = new Account();
        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setBalance(BigDecimal.ZERO);
        admin.setRole("ADMIN");
        accountRepository.save(admin);
    }
}