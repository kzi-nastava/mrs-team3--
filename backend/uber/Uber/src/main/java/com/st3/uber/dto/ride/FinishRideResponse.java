package com.st3.uber.dto.ride;

import com.st3.uber.enums.RideStatus;

import java.time.LocalDateTime;

public record FinishRideResponse(
        Long rideId,
        RideStatus status,
        LocalDateTime finishedAt,
        double finalPrice,
        boolean hasNextRide,
        Long nextRideId
) {}