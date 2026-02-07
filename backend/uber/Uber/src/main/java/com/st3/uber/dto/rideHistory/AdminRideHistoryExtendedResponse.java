package com.st3.uber.dto.rideHistory;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.ride.InconsistencyReportItemResponse;
import com.st3.uber.enums.RideStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminRideHistoryExtendedResponse extends AdminRideHistoryResponse{
  private String driverName;
  private List<String> passengerEmails;
  private List<Location> stops;
  private Double driverReview;
  private Double rideReview;
  private List<InconsistencyReportItemResponse> inconsistencyReports;
  private String cancellationReason;

public AdminRideHistoryExtendedResponse(Long id, RideStatus status, Location startLocation, Location endLocation,
                                          LocalDateTime startTime, LocalDateTime endTime, double price, boolean panic,
                                          String driverName, List<String> passengerEmails, List<Location> stops,
                                          Double driverReview, Double rideReview,
                                          List<InconsistencyReportItemResponse> inconsistencyReports,
                                          String cancellationReason) {
    super(id, status, startLocation, endLocation, startTime, endTime, price, panic);
    this.driverName = driverName;
    this.passengerEmails = passengerEmails;
    this.stops = stops;
    this.driverReview = driverReview;
    this.rideReview = rideReview;
    this.inconsistencyReports = inconsistencyReports;
    this.cancellationReason = cancellationReason;
  }

  public AdminRideHistoryExtendedResponse() {
  }
}
