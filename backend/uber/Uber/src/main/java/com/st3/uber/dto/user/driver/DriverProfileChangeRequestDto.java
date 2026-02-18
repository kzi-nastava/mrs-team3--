package com.st3.uber.dto.user.driver;

import com.st3.uber.enums.VehicleType;
import jakarta.validation.constraints.*;

public record DriverProfileChangeRequestDto(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name length invalid")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50)
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9+\\- ]{6,20}$",
                message = "Phone number format invalid"
        )
        String phoneNumber,

        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 255)
        String address,

        String profileImage, // optional

        @NotBlank(message = "Vehicle model required")
        String vehicleModel,

        @NotBlank(message = "Registration number required")
        @Size(min = 3, max = 15)
        String vehicleRegistrationNumber,

        @NotNull(message = "Seating capacity required")
        @Min(value = 1, message = "Must be >= 1")
        @Max(value = 8, message = "Too many seats")
        Integer vehicleSeatingCapacity,

        @NotNull(message = "Vehicle type required")
        VehicleType vehicleType,

        @NotNull(message = "Baby transport flag required")
        Boolean babyTransport,

        @NotNull(message = "Pet transport flag required")
        Boolean petTransport
) {}
