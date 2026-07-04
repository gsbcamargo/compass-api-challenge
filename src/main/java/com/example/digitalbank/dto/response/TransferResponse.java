package com.example.digitalbank.dto.response;

import com.example.digitalbank.domain.TransferRecord;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(UUID id, UUID fromAccountId, UUID toAccountId, BigDecimal amount, String status) {
    public static TransferResponse from(TransferRecord record) {
        return new TransferResponse(record.getId(), record.getFromAccountId(), record.getToAccountId(),
                record.getAmount(), record.getStatus().name());
    }
}
