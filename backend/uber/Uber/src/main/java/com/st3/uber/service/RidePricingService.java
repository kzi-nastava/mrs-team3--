package com.st3.uber.service;

import com.st3.uber.domain.RidePricing;
import com.st3.uber.dto.pricing.PricingChangeRequest;
import com.st3.uber.dto.pricing.PricingResponse;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.exception.PricingValidationException;
import com.st3.uber.repository.RidePricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class RidePricingService {

    private final RidePricingRepository ridePricingRepository;

    private static final double MIN_BASE_PRICE = 50.0;
    private static final double MAX_BASE_PRICE = 10000.0;
    private static final double MIN_PRICE_PER_KM = 10.0;
    private static final double MAX_PRICE_PER_KM = 1000.0;

    public PricingResponse getCurrentPricing() {
        RidePricing pricing = ridePricingRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Pricing configuration not found"));

        return mapToPricingResponse(pricing);
    }

    @Transactional
    public PricingResponse updatePricing(PricingChangeRequest request) {
        RidePricing currentPricing = ridePricingRepository.findById(1L)
                .orElseGet(() -> {
                    RidePricing newPricing = new RidePricing();
                    newPricing.setBasePrices(new HashMap<>());
                    return newPricing;
                });

        validatePricingRequest(request, currentPricing);

        if (request.standardBasePrice() != null) {
            currentPricing.getBasePrices().put(VehicleType.STANDARD, request.standardBasePrice());
        }
        if (request.luxuryBasePrice() != null) {
            currentPricing.getBasePrices().put(VehicleType.LUXURY, request.luxuryBasePrice());
        }
        if (request.vanBasePrice() != null) {
            currentPricing.getBasePrices().put(VehicleType.VAN, request.vanBasePrice());
        }
        if (request.pricePerKm() != null) {
            currentPricing.setPricePerKm(request.pricePerKm());
        }

        RidePricing savedPricing = ridePricingRepository.save(currentPricing);

        return mapToPricingResponse(savedPricing);
    }

    private void validatePricingRequest(PricingChangeRequest request, RidePricing currentPricing) {
        StringBuilder errors = new StringBuilder();

        // Calculate what the final values will be after applying changes
        Double finalStandard = request.standardBasePrice() != null
                ? request.standardBasePrice()
                : currentPricing.getBasePrices().getOrDefault(VehicleType.STANDARD, 0.0);

        Double finalLuxury = request.luxuryBasePrice() != null
                ? request.luxuryBasePrice()
                : currentPricing.getBasePrices().getOrDefault(VehicleType.LUXURY, 0.0);

        Double finalVan = request.vanBasePrice() != null
                ? request.vanBasePrice()
                : currentPricing.getBasePrices().getOrDefault(VehicleType.VAN, 0.0);

        Double finalPricePerKm = request.pricePerKm() != null
                ? request.pricePerKm()
                : currentPricing.getPricePerKm();

        if (request.standardBasePrice() != null) {
            if (request.standardBasePrice() < MIN_BASE_PRICE) {
                errors.append("Standard base price must be at least ").append(MIN_BASE_PRICE).append(". ");
            }
            if (request.standardBasePrice() > MAX_BASE_PRICE) {
                errors.append("Standard base price cannot exceed ").append(MAX_BASE_PRICE).append(". ");
            }
        }

        if (request.luxuryBasePrice() != null) {
            if (request.luxuryBasePrice() < MIN_BASE_PRICE) {
                errors.append("Luxury base price must be at least ").append(MIN_BASE_PRICE).append(". ");
            }
            if (request.luxuryBasePrice() > MAX_BASE_PRICE) {
                errors.append("Luxury base price cannot exceed ").append(MAX_BASE_PRICE).append(". ");
            }
        }

        if (request.vanBasePrice() != null) {
            if (request.vanBasePrice() < MIN_BASE_PRICE) {
                errors.append("Van base price must be at least ").append(MIN_BASE_PRICE).append(". ");
            }
            if (request.vanBasePrice() > MAX_BASE_PRICE) {
                errors.append("Van base price cannot exceed ").append(MAX_BASE_PRICE).append(". ");
            }
        }

        if (request.pricePerKm() != null) {
            if (request.pricePerKm() < MIN_PRICE_PER_KM) {
                errors.append("Price per km must be at least ").append(MIN_PRICE_PER_KM).append(". ");
            }
            if (request.pricePerKm() > MAX_PRICE_PER_KM) {
                errors.append("Price per km cannot exceed ").append(MAX_PRICE_PER_KM).append(". ");
            }
        }

        if (finalLuxury <= finalStandard) {
            errors.append(String.format(
                    "Luxury base price (%.2f) must be higher than standard base price (%.2f). ",
                    finalLuxury, finalStandard
            ));
        }

        if (finalVan <= finalLuxury) {
            errors.append(String.format(
                    "Van base price (%.2f) must be higher than luxury base price (%.2f). ",
                    finalVan, finalLuxury
            ));
        }

        if (finalVan <= finalStandard) {
            errors.append(String.format(
                    "Van base price (%.2f) must be higher than standard base price (%.2f). ",
                    finalVan, finalStandard
            ));
        }

        if (errors.length() > 0) {
            throw new PricingValidationException(errors.toString().trim());
        }
    }

    private PricingResponse mapToPricingResponse(RidePricing pricing) {
        return new PricingResponse(
                pricing.getId(),
                pricing.getBasePrices().getOrDefault(VehicleType.STANDARD, 0.0),
                pricing.getBasePrices().getOrDefault(VehicleType.LUXURY, 0.0),
                pricing.getBasePrices().getOrDefault(VehicleType.VAN, 0.0),
                pricing.getPricePerKm()
        );
    }
}