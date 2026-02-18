package com.example.uber3.network.model.tracking;

import com.example.uber3.network.model.location.LocationDto;

import java.util.List;

public class IncomingRideResponse {
    public Long id;
    public LocationDto startLocation;
    public LocationDto endLocation;
    public String startTime;

    public List<LocationDto> stops;

    public IncomingRideResponse() {}
}
