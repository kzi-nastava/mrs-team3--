package com.st3.uber.dto.auth;

import java.time.LocalDateTime;

public record EmailValidationResponse(
        String email,
        boolean available,
        LocalDateTime validUntil,
        String message
) {}