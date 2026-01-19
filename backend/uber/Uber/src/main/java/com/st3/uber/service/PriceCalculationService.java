package com.st3.uber.service;

import com.st3.uber.domain.RidePricing;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.RidePricingRepository;
import org.springframework.stereotype.Service;

@Service
public class PriceCalculationService {

    private final RidePricingRepository ridePricingRepository;

    public PriceCalculationService(RidePricingRepository ridePricingRepository) {
        this.ridePricingRepository = ridePricingRepository;
    }

    private RidePricing getPricing() {
        return ridePricingRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Ride pricing is not configured")
                );
    }

    public double getBasePrice(VehicleType vehicleType) {
        RidePricing pricing = getPricing();

        Double price = pricing.getBasePrices().get(vehicleType);
        if (price == null) {
            throw new IllegalStateException(
                    "Base price not defined for vehicle type: " + vehicleType
            );
        }
        return price;
    }

    public double calculatePrice(VehicleType vehicleType, double distanceKm) {
        RidePricing pricing = getPricing();

        double basePrice = getBasePrice(vehicleType);
        double pricePerKm = pricing.getPricePerKm();

        return basePrice + distanceKm * pricePerKm;
    }
}
