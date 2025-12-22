package com.st3.uber.dto.ride;

import com.st3.uber.enums.RideStatus;

public record StartRideResponse(
  Long rideId,
  RideStatus status,
  String startedAt
) {}
