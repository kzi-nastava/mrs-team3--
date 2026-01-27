package com.st3.uber.dto.ride;

public record TrackingTokenValidationResponse(
        boolean valid,
        String message
) {}