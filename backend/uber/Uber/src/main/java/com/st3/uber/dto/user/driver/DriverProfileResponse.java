package com.st3.uber.dto.user.driver;

import com.st3.uber.dto.vehicle.VehicleResponse;

public record DriverProfileResponse(
  Long id,
  String email,
  String firstName,
  String lastName,
  String phoneNumber,
  String address,
  String profileImage,
  VehicleResponse vehicle,
  boolean active
) {}
