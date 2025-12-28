package com.st3.uber.dto.ride;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.enums.VehicleType;

public record CreateRideRequest(
  LocationRequest startLocation,
  LocationRequest endLocation,
  VehicleType vehicleType,
  boolean babyTransport,
  boolean petTransport
) {}
