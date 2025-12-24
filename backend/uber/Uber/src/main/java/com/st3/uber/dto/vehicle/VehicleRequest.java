package com.st3.uber.dto.vehicle;

import com.st3.uber.enums.VehicleType;

public record VehicleRequest(
  String model,
  VehicleType type,
  String registrationNumber,
  int seatingCapacity,
  boolean babyTransport,
  boolean petTransport
) {}

