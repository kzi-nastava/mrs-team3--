package com.example.uber3.network.model.tracking;

import com.example.uber3.network.model.driver.Location;

import java.util.List;

public class RideTrackingData {
    public long rideId;
    public String status;
    public Location startLocation;
    public Location endLocation;
    public List<Location> stops;
    public boolean[] stopsReached;
    public Location driverCurrentLocation;
    public String driverName;
    public String driverPhone;
    public String vehicleType;
    public String vehicleModel;
    public String vehicleRegistration;
    public double distanceKm;
    public int estimatedTimeMinutes;
    public int remainingMinutes;
    public String startedAt;
    public String scheduledAt;
    public boolean stoppedEarly;
    public String createdAt;
}