package com.example.uber3.network.model.ride;

import com.example.uber3.network.model.location.LocationRequest;

import java.util.List;

public class RouteEstimateRequest {

    public LocationRequest startLocation;
    public LocationRequest endLocation;
    public List<LocationRequest> stops;
    public String vehicleType;

    public RouteEstimateRequest(
            LocationRequest start,
            LocationRequest end,
            List<LocationRequest> stops,
            String vehicleType
    ) {
        this.startLocation = start;
        this.endLocation = end;
        this.stops = stops;
        this.vehicleType = vehicleType;
    }
}
