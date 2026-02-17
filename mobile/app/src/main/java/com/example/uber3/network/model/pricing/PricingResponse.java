package com.example.uber3.network.model.pricing;

public class PricingResponse {
    public Long id;
    public double standardBasePrice;
    public double luxuryBasePrice;
    public double vanBasePrice;
    public double pricePerKm;

    public PricingResponse() {
    }

    public PricingResponse(Long id, double standardBasePrice, double luxuryBasePrice,
                           double vanBasePrice, double pricePerKm) {
        this.id = id;
        this.standardBasePrice = standardBasePrice;
        this.luxuryBasePrice = luxuryBasePrice;
        this.vanBasePrice = vanBasePrice;
        this.pricePerKm = pricePerKm;
    }
}