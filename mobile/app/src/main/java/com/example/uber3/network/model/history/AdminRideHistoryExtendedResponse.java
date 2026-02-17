package com.example.uber3.network.model.history;

import com.example.uber3.network.model.location.LocationDto;
import com.example.uber3.network.model.ride.InconsistencyReportDto;

import java.util.List;

public class AdminRideHistoryExtendedResponse extends AdminRideHistoryResponse {
    public String driverName;
    public List<String> passengerEmails;
    public List<LocationDto> stops;
    public Double driverReview;
    public Double rideReview;
    public List<InconsistencyReportDto> inconsistencyReports;
    public String cancellationReason;
}
