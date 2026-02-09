package com.st3.uber.dto.user.admin;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.route.StopStatus;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRideTrackingDto(
        Long rideId,
        RideStatus status,

        // Locations
        Location startLocation,
        Location endLocation,
        List<Location> stops,
        List<StopStatus> stopStatuses,

        // Driver info
        Location driverCurrentLocation,
        Long driverId,
        String driverName,
        String driverPhone,
        String driverEmail,

        // Vehicle info
        VehicleType vehicleType,
        String vehicleModel,
        String vehicleRegistration,

        // Ride details
        double distanceKm,
        int estimatedTimeMinutes,
        int remainingMinutes,
        double calculatedPrice,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,

        // Special features
        boolean babyTransport,
        boolean petTransport,

        // Passengers and invites
        List<PassengerInfo> passengers,
        Long creatorId,
        String creatorName,
        List<RideInviteInfo> invites,

        // Panic status
        boolean panic
) {
    public record PassengerInfo(
            Long id,
            String name,
            String surname,
            String email
    ) {}

    public record RideInviteInfo(
            Long id,
            String email,
            String trackingToken,
            LocalDateTime createdAt
    ) {}
}