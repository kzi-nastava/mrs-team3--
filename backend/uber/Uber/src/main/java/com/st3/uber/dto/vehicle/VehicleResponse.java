package com.st3.uber.dto.vehicle;

import com.st3.uber.enums.VehicleType;

public record VehicleResponse(
  Long id,
  String model,
  VehicleType type
) {}
