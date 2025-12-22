package com.st3.uber.dto.register;

import com.st3.uber.dto.vehicle.VehicleRequest;
import com.st3.uber.enums.VehicleType;

public record RegisterDriverRequest(
  String email,
  String password,
  String firstName,
  String lastName,
  String phoneNumber,
  String address,
  VehicleRequest request
) {}


