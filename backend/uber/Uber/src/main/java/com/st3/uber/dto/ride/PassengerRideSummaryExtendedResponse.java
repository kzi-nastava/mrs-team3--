package com.st3.uber.dto.ride;

import com.st3.uber.domain.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PassengerRideSummaryExtendedResponse extends PassengerRideSummaryResponse {
  private List<Location> stops;
  private Driver driver;
  private List<Review> reviews;
  private List<InconsistencyReport> inconsistencyReports;
}
