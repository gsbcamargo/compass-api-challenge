package com.example.digitalbank.controller;

import com.example.digitalbank.dto.response.AccountResponse;
import com.example.digitalbank.dto.response.TransferResponse;
import com.example.digitalbank.repository.AccountRepository;
import com.example.digitalbank.repository.TransferRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AccountRepository accountRepository;
    private final TransferRecordRepository transferRecordRepository;

    public AdminController(AccountRepository accountRepository, TransferRecordRepository transferRecordRepository) {
        this.accountRepository = accountRepository;
        this.transferRecordRepository = transferRecordRepository;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> listAllAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/transfers")
    public List<TransferResponse> listAllTransfers() {
        return transferRecordRepository.findAll().stream().map(TransferResponse::from).toList();
    }
}