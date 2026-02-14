package com.example.uber3.network.model.driver;


public class FinishRideResponse {
    public long rideId;
    public String status;
    public String startedAt;
    public String finishedAt;
    public double calculatedPrice;
    public boolean hasNextRide;
    public Long nextRideId;

    public FinishRideResponse() {}
}