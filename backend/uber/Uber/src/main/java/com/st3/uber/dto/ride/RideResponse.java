package com.st3.uber.dto.ride;

import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;

public record RideResponse(
        Long rideId,
        RideStatus status,
        double distanceKm,
        int estimatedTimeMinutes,
        double calculatedPrice,
        VehicleType vehicleType
) {}
