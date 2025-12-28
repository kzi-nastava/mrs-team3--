package com.st3.uber.dto.ride;

import java.time.LocalDateTime;

// POST /api/rides/{id}/rating - Response
public record SubmitRatingResponse(
        Long rideId,
        Integer driverRating,
        Integer vehicleRating,
        String comment,
        LocalDateTime reviewedAt,
        String message
) {}