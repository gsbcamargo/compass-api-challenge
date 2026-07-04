package com.example.digitalbank.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Account with id " + accountId + " not found");
    }


}
