package com.st3.uber.dto.ride;

import java.time.LocalDateTime;

// GET /api/rides/{id}/location - Response
public record RideLocationResponse(
        Long rideId,
        double currentLatitude,
        double currentLongitude,
        String currentAddress,
        int estimatedMinutesToArrival,
        LocalDateTime lastUpdated
) {}