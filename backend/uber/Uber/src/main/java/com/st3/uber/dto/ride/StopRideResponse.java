package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StopRideResponse {
  private Long rideId;
  private LocalDateTime stoppedAt;
  private Double distanceMeters;
  private Double price;
  private Location actualEndLocation;
  private boolean stoppedEarly;


  public StopRideResponse(Long rideId, LocalDateTime stoppedAt, Double distanceMeters, Double price, StopRideRequest request) {
    this.rideId = rideId;
    this.stoppedAt = stoppedAt;
    this.distanceMeters = distanceMeters;
    this.price = price;
    this.actualEndLocation = request.getActualEndLocation();
    this.stoppedEarly = true;
  }
}
