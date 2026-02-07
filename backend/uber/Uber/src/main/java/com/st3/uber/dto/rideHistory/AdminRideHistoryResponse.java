package com.st3.uber.dto.rideHistory;

import com.st3.uber.enums.RideStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminRideHistoryResponse extends PassengerRideSummaryResponse{
  private double price;
  private boolean panic;

  public AdminRideHistoryResponse(Long id, double price, boolean panic) {
    super();
    this.setId(id);
    this.price = price;
    this.panic = panic;
  }

  public AdminRideHistoryResponse() {

  }
}
