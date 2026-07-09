package com.example.digitalbank.controller;

import com.example.digitalbank.domain.TransferRecord;
import com.example.digitalbank.dto.request.TransferRequest;
import com.example.digitalbank.dto.response.TransferResponse;
import com.example.digitalbank.exception.ForbiddenAccessException;
import com.example.digitalbank.security.utils.SecurityUtils;
import com.example.digitalbank.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest transferRequest,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                     Authentication authentication) {
        if (!SecurityUtils.canAccess(transferRequest.fromAccountId(), authentication)) {
            throw new ForbiddenAccessException("You can only transfer from your own account");
        }

        TransferRecord transferRecord = transferService.transfer(
                transferRequest.fromAccountId(), transferRequest.toAccountId(), transferRequest.amount(), idempotencyKey);

        return TransferResponse.from(transferRecord);
    }
}
