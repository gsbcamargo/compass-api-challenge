package com.example.digitalbank.controller;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.dto.request.CreateAccountRequest;
import com.example.digitalbank.dto.response.AccountResponse;
import com.example.digitalbank.dto.response.AccountSummaryResponse;
import com.example.digitalbank.exception.AccountNotFoundException;
import com.example.digitalbank.exception.ForbiddenAccessException;
import com.example.digitalbank.repository.AccountRepository;
import com.example.digitalbank.security.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<AccountSummaryResponse> getAccounts() {
        return accountRepository.findAll().stream()
                .map(a -> new AccountSummaryResponse(a.getId(), a.getFirstName(), a.getLastName()))
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id, Authentication authentication) {
        if (!SecurityUtils.canAccess(id, authentication)) {
            throw new ForbiddenAccessException("You can only view your own account");
        }

        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = new Account();
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setUsername(request.username());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setBalance(request.initialBalance());
        account.setRole("USER");

        return AccountResponse.from(accountRepository.save(account));
    }
}
