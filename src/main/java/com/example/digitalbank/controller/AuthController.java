package com.example.digitalbank.controller;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.dto.request.LoginRequest;
import com.example.digitalbank.dto.response.LoginResponse;
import com.example.digitalbank.repository.AccountRepository;
import com.example.digitalbank.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, AccountRepository accountRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        Account account = accountRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated account not found"));

        String token = jwtService.generateToken(account.getId(), account.getUsername());

        return new LoginResponse(token, account.getId(), account.getFirstName());
    }
}
