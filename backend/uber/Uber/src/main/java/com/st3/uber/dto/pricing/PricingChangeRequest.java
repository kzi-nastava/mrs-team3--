package com.st3.uber.dto.pricing;
import jakarta.validation.constraints.*;

public record PricingChangeRequest(
        @DecimalMin(value = "50.0", message = "Standard base price must be at least 50.0")
        @DecimalMax(value = "10000.0", message = "Standard base price cannot exceed 10000.0")
        Double standardBasePrice,

        @DecimalMin(value = "50.0", message = "Luxury base price must be at least 50.0")
        @DecimalMax(value = "10000.0", message = "Luxury base price cannot exceed 10000.0")
        Double luxuryBasePrice,

        @DecimalMin(value = "50.0", message = "Van base price must be at least 50.0")
        @DecimalMax(value = "10000.0", message = "Van base price cannot exceed 10000.0")
        Double vanBasePrice,

        @DecimalMin(value = "10.0", message = "Price per km must be at least 10.0")
        @DecimalMax(value = "1000.0", message = "Price per km cannot exceed 1000.0")
        Double pricePerKm
) {}