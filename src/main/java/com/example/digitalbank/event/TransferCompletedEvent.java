package com.example.digitalbank.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount

) {}
