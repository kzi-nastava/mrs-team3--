package com.st3.uber.dto.ride;

import com.st3.uber.enums.RideStatus;
import java.time.LocalDateTime;

// POST /api/rides/{id}/complete-detailed - Response
public record CompleteRideResponse(
        Long rideId,
        RideStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String startAddress,
        String endAddress,
        double finalPrice,
        double totalDistance
) {}