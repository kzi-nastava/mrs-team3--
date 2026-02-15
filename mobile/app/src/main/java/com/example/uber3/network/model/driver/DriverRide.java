package com.example.uber3.network.model.driver;

import java.util.List;

public class DriverRide {
    public long rideId;
    public String status; // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    public Location startLocation;
    public Location endLocation;
    public List<Location> stops;
    public List<StopStatus> stopStatuses;
    public Location driverCurrentLocation;
    public int estimatedTimeMinutes;
    public int remainingMinutes;
    public String scheduledAt;
    public String startedAt;
    public double distance;
    public double calculatedPrice;
    public String vehicleType;
    public boolean babyTransport;
    public boolean petTransport;
    public int passengerCount;
    public String creatorName;

    public DriverRide() {}
}