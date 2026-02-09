package com.example.uber3.network.model.favorite;

import com.example.uber3.network.model.location.LocationRequest;

import java.util.List;

public class FavoriteRouteRequest {

    public Long rideId;
    public LocationRequest from;
    public LocationRequest to;
    public List<LocationRequest> stops;
    public String vehicleType;
    public boolean babyTransport;
    public boolean petTransport;

}
