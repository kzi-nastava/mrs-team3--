package com.example.uber3.network.model.admin;

import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.driver.StopStatus;

import java.util.List;

public class AdminRideTrackingData {
    public long rideId;
    public String status;
    public Location startLocation;
    public Location endLocation;
    public List<Location> stops;
    public List<StopStatus> stopStatuses;
    public Location driverCurrentLocation;
    public long driverId;
    public String driverName;
    public String driverPhone;
    public String driverEmail;
    public String vehicleType;
    public String vehicleModel;
    public String vehicleRegistration;
    public double distanceKm;
    public int estimatedTimeMinutes;
    public int remainingMinutes;
    public double calculatedPrice;
    public String createdAt;
    public String scheduledAt;
    public String startedAt;
    public String finishedAt;
    public boolean babyTransport;
    public boolean petTransport;
    public List<PassengerInfo> passengers;
    public long creatorId;
    public String creatorName;
    public List<RideInviteInfo> invites;
    public boolean panic;
}