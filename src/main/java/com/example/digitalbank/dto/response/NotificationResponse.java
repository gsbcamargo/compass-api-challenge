package com.example.digitalbank.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String message, Instant createdAt) {}
