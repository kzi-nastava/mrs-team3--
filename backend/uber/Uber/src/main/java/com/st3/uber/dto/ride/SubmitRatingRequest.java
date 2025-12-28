package com.st3.uber.dto.ride;


// POST /api/rides/{id}/rating - Request
public record SubmitRatingRequest(
        Integer driverRating,    // 1-5 or null
        Integer vehicleRating,   // 1-5 or null
        String comment           // optional
) {}