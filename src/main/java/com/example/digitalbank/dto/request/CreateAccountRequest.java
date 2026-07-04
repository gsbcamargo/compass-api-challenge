package com.example.digitalbank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateAccountRequest (
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotNull @PositiveOrZero
        BigDecimal initialBalance
){}
