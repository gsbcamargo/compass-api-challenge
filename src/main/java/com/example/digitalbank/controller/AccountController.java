package com.example.digitalbank.controller;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.dto.request.CreateAccountRequest;
import com.example.digitalbank.dto.response.AccountResponse;
import com.example.digitalbank.repository.AccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<AccountResponse> getAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                // TODO refactor exception
                .orElseThrow(() -> new RuntimeException("Account not found" + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = new Account();
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setBalance(request.initialBalance());
        return AccountResponse.from(accountRepository.save(account));
    }
}
