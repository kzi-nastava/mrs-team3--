package com.example.uber3.network.model.report;

import java.util.List;

public class RideReportResponse {

    public List<DailyReportItem> daily;

    public long totalRides;
    public double totalDistance;
    public double totalMoney;

    public double avgRidesPerDay;
    public double avgDistancePerDay;
    public double avgMoneyPerDay;
}
