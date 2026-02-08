package com.st3.uber.dto.pricing;

public record PricingChangeRequest(
        Double standardBasePrice,
        Double luxuryBasePrice,
        Double vanBasePrice,
        Double pricePerKm
) {
}