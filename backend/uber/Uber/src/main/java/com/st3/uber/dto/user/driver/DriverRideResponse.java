package com.st3.uber.dto.user.driver;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.route.StopStatus;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public record DriverRideResponse(
        Long rideId,
        RideStatus status,
        Location startLocation,
        Location endLocation,
        List<Location> stops,
        List<StopStatus> stopStatuses,
        Location driverCurrentLocation,
        int estimatedTimeMinutes,
        int remainingMinutes,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        double distance,
        double calculatedPrice,
        VehicleType vehicleType,
        boolean babyTransport,
        boolean petTransport,
        int passengerCount,
        String creatorName
) {}