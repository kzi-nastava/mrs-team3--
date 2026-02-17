package com.example.uber3.network.model.pricing;

public class PricingChangeRequest {
    public Double standardBasePrice;
    public Double luxuryBasePrice;
    public Double vanBasePrice;
    public Double pricePerKm;

    public PricingChangeRequest() {
    }

    public PricingChangeRequest(Double standardBasePrice, Double luxuryBasePrice,
                                Double vanBasePrice, Double pricePerKm) {
        this.standardBasePrice = standardBasePrice;
        this.luxuryBasePrice = luxuryBasePrice;
        this.vanBasePrice = vanBasePrice;
        this.pricePerKm = pricePerKm;
    }
}