package com.example.uber3.network.model.driver;

import java.util.List;

public class PendingRide {
    public long rideId;
    public Location startLocation;
    public Location endLocation;
    public List<Location> stops;
    public int estimatedTimeMinutes;
    public String createdAt;
    public double distance;
    public double calculatedPrice;
    public String vehicleType;
    public boolean babyTransport;
    public boolean petTransport;
    public int passengerCount;

    public PendingRide() {}
}