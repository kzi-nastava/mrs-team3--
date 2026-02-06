package com.st3.uber.dto.rideHistory;

import com.st3.uber.enums.RideStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminRideHistoryResponse extends PassengerRideSummaryResponse{
  private RideStatus status;
  private double price;

  public AdminRideHistoryResponse(Long id, RideStatus status, double price) {
    super();
    this.setId(id);
    this.status = status;
    this.price = price;
  }

  public AdminRideHistoryResponse() {

  }
}
