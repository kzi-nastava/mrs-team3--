package com.st3.uber.dto.ride;

import java.time.LocalDateTime;
import java.util.List;

public record RideReviewDetailResponse(
        Long rideId,
        LocationDto startLocation,
        LocationDto endLocation,
        List<LocationDto> stops,
        String driverName,
        String vehicleType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        double distance,
        int duration,
        double price,
        boolean canReview,           // false if 3 days passed
        LocalDateTime reviewDeadline,
        ExistingReviewDto existingReview  // null if no review yet
) {
    public record LocationDto(
            double latitude,
            double longitude,
            String address
    ) {}

    public record ExistingReviewDto(
            Integer driverRating,
            Integer vehicleRating,
            String comment,
            LocalDateTime reviewedAt
    ) {}
}