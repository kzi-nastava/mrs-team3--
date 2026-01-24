package com.st3.uber.dto.user.admin;

import java.time.LocalDateTime;

public record AdminDriverProfileChangeRequestDto(
        Long requestId,
        Long driverId,
        String driverEmail,
        String driverName,
        String driverSurname,
        LocalDateTime requestedAt,
        String status
) {}
