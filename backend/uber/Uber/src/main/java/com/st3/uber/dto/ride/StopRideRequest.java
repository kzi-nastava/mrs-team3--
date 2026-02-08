package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import lombok.Data;

@Data
public class StopRideRequest {
  private Location actualEndLocation;

  public StopRideRequest(Long rideId, Location actualEndLocation) {
    this.actualEndLocation = actualEndLocation;
  }
}
