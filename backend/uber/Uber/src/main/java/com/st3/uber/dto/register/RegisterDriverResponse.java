package com.st3.uber.dto.register;

import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.VehicleType;

public record RegisterDriverResponse(
  Long id,
  String email,
  String firstName,
  String lastName,
  VehicleResponse vehicleResponse,
  boolean active
) {}
