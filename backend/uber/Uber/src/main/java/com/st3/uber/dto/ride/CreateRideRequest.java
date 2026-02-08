package com.st3.uber.dto.ride;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public record CreateRideRequest(
        LocationRequest startLocation,
        LocationRequest endLocation,

        List<LocationRequest> stops,

        List<String> passengerEmails,

        VehicleType vehicleType,
        boolean babyTransport,
        boolean petTransport,

        LocalDateTime scheduledAt
) {}
