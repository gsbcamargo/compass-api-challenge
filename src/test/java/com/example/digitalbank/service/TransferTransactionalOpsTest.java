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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferTransactionalOpsTest {

    @Mock AccountRepository accountRepository;
    @Mock TransferRecordRepository transferRecordRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    TransferTransactionalOps ops;

    @BeforeEach
    void setUp() {
        ops = new TransferTransactionalOps(accountRepository, transferRecordRepository, eventPublisher);
    }

    @Test
    void transfersAmountBetweenAccounts() {
        Account alice = account(new BigDecimal("100.00"));
        Account bruno = account(new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(alice.getId())).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdForUpdate(bruno.getId())).thenReturn(Optional.of(bruno));

        TransferRecord result = ops.execute(alice.getId(), bruno.getId(), new BigDecimal("30.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("70.00");
        assertThat(bruno.getBalance()).isEqualByComparingTo("80.00");
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getFromAccountId()).isEqualTo(alice.getId());
        assertThat(result.getToAccountId()).isEqualTo(bruno.getId());
        verify(transferRecordRepository).save(any(TransferRecord.class));
        verify(eventPublisher).publishEvent(any(TransferCompletedEvent.class));
    }

    @Test
    void rejectsInsufficientFunds() {
        Account alice = account(new BigDecimal("10.00"));
        Account bruno = account(new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(alice.getId())).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdForUpdate(bruno.getId())).thenReturn(Optional.of(bruno));

        assertThrows(InsufficientFundsException.class, () ->
                ops.execute(alice.getId(), bruno.getId(), new BigDecimal("100.00")));

        // balances must be untouched when a transfer is rejected mid-way
        assertThat(alice.getBalance()).isEqualByComparingTo("10.00");
        assertThat(bruno.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejectsSameAccountTransfer() {
        UUID id = UUID.randomUUID();
        assertThrows(InvalidTransferException.class, () -> ops.execute(id, id, BigDecimal.TEN));
    }

    @Test
    void rejectsZeroAmount() {
        assertThrows(InvalidTransferException.class, () ->
                ops.execute(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(InvalidTransferException.class, () ->
                ops.execute(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-5.00")));
    }

    @Test
    void throwsWhenAccountDoesNotExist() {
        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                ops.execute(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN));
    }

    private Account account(BigDecimal balance) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setFirstName("Test");
        a.setLastName("User");
        a.setBalance(balance);
        return a;
    }
}