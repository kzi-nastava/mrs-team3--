package com.st3.uber.dto.route;

public record RouteEstimateResponse(
        double distanceKm,
        int estimatedTimeMinutes,
        double estimatedPrice
) {}
