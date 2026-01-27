package com.st3.uber.dto.vehicle;

public record ActiveVehicleResponse(
        double latitude,
        double longitude,
        boolean available,  // true = green (available), false = red (unavailable)
        String registrationNumber
) {}