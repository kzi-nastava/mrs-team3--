package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import com.st3.uber.enums.CancelledBy;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminRideDetailsResponse {
  LocalDateTime rideStartTime;
  LocalDateTime rideEndTime;
  List<Location> route;
  Location startLocation;
  Location endLocation;
  CancelledBy cancelledBy;
  double price;
  boolean isPanicPressed;

  public AdminRideDetailsResponse(LocalDateTime rideStartTime, LocalDateTime rideEndTime, List<Location> route,
                                  Location startLocation, Location endLocation, CancelledBy cancelledBy,
                                  double price, boolean isPanicPressed) {
    this.rideStartTime = rideStartTime;
    this.rideEndTime = rideEndTime;
    this.route = route;
    this.startLocation = startLocation;
    this.endLocation = endLocation;
    this.cancelledBy = cancelledBy;
    this.price = price;
    this.isPanicPressed = isPanicPressed;
  }
}


