package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public record PendingRideResponse(
        Long rideId,
        Location startLocation,
        Location endLocation,
        List<Location> stops,
        int estimatedTimeMinutes,
        LocalDateTime createdAt,
        double distance,
        double calculatedPrice,
        VehicleType vehicleType,
        boolean babyTransport,
        boolean petTransport,
        int passengerCount
) {}