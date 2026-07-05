package com.example.digitalbank.dto.response;

import com.example.digitalbank.domain.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record AccountResponse(UUID id, String firstName, String lastName, BigDecimal balance) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getFirstName(), account.getLastName(),
                account.getBalance().setScale(2, RoundingMode.HALF_UP));
    }
}
