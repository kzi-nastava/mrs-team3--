package com.st3.uber.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyReportItem {

    private LocalDate date;

    private long rideCount;

    private double totalDistance;

    private double totalMoney;
}
