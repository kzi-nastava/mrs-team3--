package com.st3.uber.dto.rideHistory;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.InconsistencyReportItemResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PassengerRideSummaryExtendedResponse extends PassengerRideSummaryResponse {
  private List<Location> stops;
  private String driverName;
  private Double driverReview;
  private Double rideReview;
  private List<InconsistencyReportItemResponse> inconsistencyReports;
}
