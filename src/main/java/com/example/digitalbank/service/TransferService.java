package com.example.digitalbank.service;

import com.example.digitalbank.domain.TransferRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferTransactionalOps ops;

    public TransferService(TransferTransactionalOps ops) {
        this.ops = ops;
    }

    public TransferRecord transfer(UUID fromId, UUID toId, BigDecimal amount) {
        return  ops.execute(fromId, toId, amount);
    }
}
