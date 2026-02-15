package com.example.uber3.network.model.driver;


public class FinishRideRequest {
    public Location actualEndLocation;

    public FinishRideRequest(Location actualEndLocation) {
        this.actualEndLocation = actualEndLocation;
    }
}