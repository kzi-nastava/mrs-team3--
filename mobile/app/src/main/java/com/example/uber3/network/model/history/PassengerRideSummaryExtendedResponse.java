package com.example.uber3.network.model.history;

import com.example.uber3.network.model.location.LocationDto;
import com.example.uber3.network.model.ride.InconsistencyReportDto;

import java.util.List;

public class PassengerRideSummaryExtendedResponse
        extends PassengerRideSummaryResponse {

    public List<LocationDto> stops;
    public String driverName;
    public Double driverReview;
    public Double rideReview;
    public List<InconsistencyReportDto> inconsistencyReports;
}
