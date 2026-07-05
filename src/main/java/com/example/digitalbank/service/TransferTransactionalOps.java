package com.example.digitalbank.service;

import com.example.digitalbank.domain.Account;
import com.example.digitalbank.domain.TransferRecord;
import com.example.digitalbank.domain.TransferStatus;
import com.example.digitalbank.event.TransferCompletedEvent;
import com.example.digitalbank.exception.AccountNotFoundException;
import com.example.digitalbank.exception.InsufficientFundsException;
import com.example.digitalbank.exception.InvalidTransferException;
import com.example.digitalbank.repository.AccountRepository;
import com.example.digitalbank.repository.TransferRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

@Component
class TransferTransactionalOps {
    private static final Logger log = LoggerFactory.getLogger(TransferTransactionalOps.class);

    private final AccountRepository accountRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferTransactionalOps(AccountRepository accountRepository, TransferRecordRepository transferRecordRepository, ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transferRecordRepository = transferRecordRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    TransferRecord execute(UUID fromId, UUID toId, BigDecimal amount) {
        if(fromId.equals(toId)) {
            throw new InvalidTransferException("Origin and destination accounts must differ");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero");
        }

        UUID first = fromId.compareTo(toId) > 0 ? fromId : toId;
        UUID second = fromId.compareTo(toId) > 0 ? toId : fromId;

        Account firstLocked = accountRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new AccountNotFoundException(first));
        Account secondLocked = accountRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new AccountNotFoundException(second));

        Account source = fromId.equals(first) ? firstLocked : secondLocked;
        Account destination = toId.equals(second) ? secondLocked : firstLocked;

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(source.getId());
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        accountRepository.save(source);
        accountRepository.save(destination);

        TransferRecord transferRecord = new TransferRecord();
        transferRecord.setFromAccountId(fromId);
        transferRecord.setToAccountId(toId);
        transferRecord.setAmount(amount);
        transferRecord.setStatus(TransferStatus.COMPLETED);

        transferRecordRepository.save(transferRecord);

        log.info("About to publish TransferCompletedEvent for transfer {}", transferRecord.getId());
        eventPublisher.publishEvent(new TransferCompletedEvent(transferRecord.getId(),fromId, toId, amount));

        return transferRecord;
    }
}
