package com.example.digitalbank.repository;

import com.example.digitalbank.domain.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, UUID> {

    Optional<TransferRecord> findByIdempotencyKey(String idempotencyKey);

}

