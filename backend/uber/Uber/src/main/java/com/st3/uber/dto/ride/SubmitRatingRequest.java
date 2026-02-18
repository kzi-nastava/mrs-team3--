package com.st3.uber.dto.ride;


import jakarta.validation.constraints.*;

public record SubmitRatingRequest(
        @NotNull(message = "Driver rating is required")
        @Min(value = 1, message = "Driver rating must be between 1 and 5")
        @Max(value = 5, message = "Driver rating must be between 1 and 5")
        Integer driverRating,

        @NotNull(message = "Vehicle rating is required")
        @Min(value = 1, message = "Vehicle rating must be between 1 and 5")
        @Max(value = 5, message = "Vehicle rating must be between 1 and 5")
        Integer vehicleRating,

        @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
        String comment
) {}