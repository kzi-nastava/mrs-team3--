package com.example.uber3.network.model.history;

import com.example.uber3.network.model.location.LocationDto;
import com.example.uber3.network.model.ride.InconsistencyReportDto;
import com.example.uber3.network.model.ride.ReviewDto;
import java.util.List;

public class DriverRideHistoryDetailResponse {

    public Long rideId;

    public String startAddress;

    public Double startLatitude;

    public Double startLongitude;

    public String endAddress;

    public Double endLatitude;

    public Double endLongitude;

    public String startedAt;

    public String finishedAt;

    public String status;

    public Double price;

    public Double distance;

    public String vehicleType;

    public boolean wasCancelled;

    public String cancelledBy;

    public String terminationReason;

    public boolean hadPanicEvent;

    public boolean wasFinishedEarly;

    public List<String> passengerNames;

    public List<String> invitedPassengers;

    public List<LocationDto> plannedStops;

    public List<LocationDto> actualStops;

    public List<ReviewDto> reviews;

    public List<InconsistencyReportDto> inconsistencyReports;



    public int getDurationMinutes() {
        if (startedAt == null || finishedAt == null) {
            return 0;
        }
        try {
            long start = java.time.Instant.parse(startedAt).toEpochMilli();
            long end = java.time.Instant.parse(finishedAt).toEpochMilli();
            return (int) ((end - start) / 60000);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getFormattedStatus() {
        if (wasFinishedEarly) {
            return "FINISHED_EARLY";
        }
        if (wasCancelled) {
            if ("DRIVER".equals(cancelledBy)) {
                return "CANCELLED_BY_DRIVER";
            } else if ("PASSENGER".equals(cancelledBy)) {
                return "CANCELLED_BY_PASSENGER";
            }
            return "CANCELLED";
        }
        return status != null ? status : "COMPLETED";
    }

    public int getTotalPassengerCount() {
        int count = 0;
        if (passengerNames != null) count += passengerNames.size();
        if (invitedPassengers != null) count += invitedPassengers.size();
        return count;
    }
}