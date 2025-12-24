package com.st3.uber.dto.ride;

import com.st3.uber.enums.VehicleType;

public record CreateRideFromFavoriteRequest(
  VehicleType vehicleType,
  boolean babyTransport,
  boolean petTransport
) {}
