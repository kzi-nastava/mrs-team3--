package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public record RideTrackingResponse(
        Long rideId,
        RideStatus status,
        Location startLocation,
        Location endLocation,
        List<Location> stops,
        Location driverCurrentLocation,
        String driverName,
        String driverPhone,
        VehicleType vehicleType,
        String vehicleModel,
        String vehicleRegistration,
        double distanceKm,
        int estimatedTimeMinutes,
        int remainingMinutes,
        LocalDateTime startedAt,
        LocalDateTime scheduledAt,
        boolean stoppedEarly,
        LocalDateTime createdAt
) {}