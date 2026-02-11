package com.example.uber3.network.model.history;

import com.example.uber3.network.model.location.LocationDto;

public class PassengerRideSummaryResponse {
    public Long id;
    public String status;
    public LocationDto startLocation;
    public LocationDto endLocation;
    public String startTime;
    public String endTime;
    public boolean favorite;
}
