package com.st3.uber.dto.ride;

import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;

public record RideResponse(
  Long rideId,
  RideStatus status,
  int estimatedTimeMinutes,
  double estimatedPrice,
  VehicleType vehicleType
) {}
