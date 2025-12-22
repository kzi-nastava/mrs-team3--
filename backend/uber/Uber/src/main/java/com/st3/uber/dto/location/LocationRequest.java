package com.st3.uber.dto.location;

public record LocationRequest(
  double latitude,
  double longitude,
  String address
) {}
