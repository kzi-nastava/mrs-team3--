package com.st3.uber.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RideReportResponse {

    private List<DailyReportItem> daily;

    private long totalRides;
    private double totalDistance;
    private double totalMoney;

    private double avgRidesPerDay;
    private double avgDistancePerDay;
    private double avgMoneyPerDay;
}
