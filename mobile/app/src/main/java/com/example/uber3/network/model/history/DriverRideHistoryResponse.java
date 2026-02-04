package com.example.uber3.network.model.history;
import java.util.List;

public class DriverRideHistoryResponse {

    public Long rideId;

    public String startAddress;

    public String endAddress;

    public String startedAt;

    public String finishedAt;

    public String status;

    public Double price;

    public Double distance;

    public boolean wasCancelled;

    public String cancelledBy;

    public boolean hadPanicEvent;

    public List<String> passengerNames;


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
}