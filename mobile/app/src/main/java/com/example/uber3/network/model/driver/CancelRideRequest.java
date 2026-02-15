package com.example.uber3.network.model.driver;


public class CancelRideRequest {
    public String cancellationReason;

    public CancelRideRequest(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}