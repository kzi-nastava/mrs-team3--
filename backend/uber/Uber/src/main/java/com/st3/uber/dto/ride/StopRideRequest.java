package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StopRideRequest {

  @NotNull(message = "Actual end location is required")
  private Location actualEndLocation;

  public StopRideRequest(Long rideId, Location actualEndLocation) {
    this.actualEndLocation = actualEndLocation;
  }
}
