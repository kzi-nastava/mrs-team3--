package com.st3.uber.dto.pricing;

public record PricingResponse(
        Long id,
        Double standardBasePrice,
        Double luxuryBasePrice,
        Double vanBasePrice,
        Double pricePerKm
) {
}