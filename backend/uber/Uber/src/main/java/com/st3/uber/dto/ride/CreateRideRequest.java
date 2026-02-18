package com.st3.uber.dto.ride;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.enums.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record CreateRideRequest(

        @NotNull(message = "Start location is required")
        @Valid
        LocationRequest startLocation,

        @NotNull(message = "End location is required")
        @Valid
        LocationRequest endLocation,

        @Valid
        List<@Valid LocationRequest> stops,

        @NotEmpty(message = "At least one passenger is required")
        List<@Email(message = "Invalid passenger email") String> passengerEmails,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        boolean babyTransport,

        boolean petTransport,

        @Future(message = "Scheduled time must be in the future")
        LocalDateTime scheduledAt
) {}
