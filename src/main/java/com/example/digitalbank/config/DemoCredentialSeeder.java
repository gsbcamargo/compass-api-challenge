package com.example.digitalbank.config;

import com.example.digitalbank.repository.AccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DemoCredentialSeeder implements ApplicationRunner {

    private static final Map<String, String> DEMO_LOGINS = Map.of(
            "Alice", "alice", "Bruno", "bruno", "Carla", "carla");
    private static final String DEMO_PASSWORD = "password123";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoCredentialSeeder(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }



    @Override
    public void run(ApplicationArguments args) {
        accountRepository.findAll().stream()
                .filter(a -> a.getUsername() == null)
                .forEach(a -> {
                    a.setUsername(DEMO_LOGINS.getOrDefault(a.getFirstName(), "user-" + a.getId().toString().substring(0, 8)));
                    a.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                    accountRepository.save(a);
                });
    }
}
