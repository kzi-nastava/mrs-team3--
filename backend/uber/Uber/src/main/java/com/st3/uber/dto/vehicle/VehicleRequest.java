package com.st3.uber.dto.vehicle;

import com.st3.uber.enums.VehicleType;
import jakarta.validation.constraints.*;

public record VehicleRequest(

        @NotBlank(message = "Model is required")
        String model,

        @NotNull(message = "Vehicle type is required")
        VehicleType type,

        @NotBlank(message = "Registration number is required")
        @Size(min = 3, max = 15, message = "Registration number length invalid")
        String registrationNumber,

        @Min(value = 1, message = "Seating capacity must be >= 1")
        @Max(value = 8, message = "Seating capacity too large")
        int seatingCapacity,

        boolean babyTransport,

        boolean petTransport
) {}
