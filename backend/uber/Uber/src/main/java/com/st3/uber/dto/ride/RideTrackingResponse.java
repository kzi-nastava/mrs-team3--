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
        List<Boolean> stopsReached,
        Location driverCurrentLocation,
        String driverName,
        String driverPhone,
        VehicleType vehicleType,
        String vehicleModel,
        String vehicleRegistration,
        Double distanceKm,
        Integer estimatedTimeMinutes,
        Integer remainingMinutes,
        LocalDateTime startedAt,
        LocalDateTime scheduledAt,
        Boolean stoppedEarly,
        LocalDateTime createdAt
) {}