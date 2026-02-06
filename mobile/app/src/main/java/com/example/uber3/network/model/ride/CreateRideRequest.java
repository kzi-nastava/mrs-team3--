package com.example.uber3.network.model.ride;

import com.example.uber3.network.model.location.LocationRequest;

import java.util.List;

public class CreateRideRequest {

    public LocationRequest startLocation;
    public LocationRequest endLocation;
    public List<LocationRequest> stops;

    public List<String> passengerEmails;

    public String vehicleType;
    public boolean babyTransport;
    public boolean petTransport;

    public String scheduledAt;

    public CreateRideRequest(
            LocationRequest start,
            LocationRequest end,
            List<LocationRequest> stops,
            List<String> passengers,
            String vehicleType,
            boolean baby,
            boolean pet,
            String scheduledAt
    ) {
        this.startLocation = start;
        this.endLocation = end;
        this.stops = stops;
        this.passengerEmails = passengers;
        this.vehicleType = vehicleType;
        this.babyTransport = baby;
        this.petTransport = pet;
        this.scheduledAt = scheduledAt;
    }
}
