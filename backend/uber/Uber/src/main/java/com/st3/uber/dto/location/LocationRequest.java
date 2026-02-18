package com.st3.uber.dto.location;

import jakarta.validation.constraints.*;

public record LocationRequest(

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        double longitude,

        @NotBlank(message = "Address is required")
        @Size(min = 3, max = 255, message = "Address length invalid")
        String address
) {}
