package com.example.digitalbank.dto.response;

import java.util.UUID;

public record LoginResponse(String accessToken, UUID accountId, String firstName) {}
