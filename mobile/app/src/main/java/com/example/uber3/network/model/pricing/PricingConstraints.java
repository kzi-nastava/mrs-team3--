package com.example.uber3.network.model.pricing;

public class PricingConstraints {
    public double minBasePrice;
    public double maxBasePrice;
    public double minPricePerKm;
    public double maxPricePerKm;

    public PricingConstraints() {
    }

    public PricingConstraints(double minBasePrice, double maxBasePrice,
                              double minPricePerKm, double maxPricePerKm) {
        this.minBasePrice = minBasePrice;
        this.maxBasePrice = maxBasePrice;
        this.minPricePerKm = minPricePerKm;
        this.maxPricePerKm = maxPricePerKm;
    }
}