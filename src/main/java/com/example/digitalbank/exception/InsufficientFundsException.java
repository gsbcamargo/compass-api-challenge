package com.example.digitalbank.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID id) {
        super("Insufficient funds for account with id: " + id);
    }

}
