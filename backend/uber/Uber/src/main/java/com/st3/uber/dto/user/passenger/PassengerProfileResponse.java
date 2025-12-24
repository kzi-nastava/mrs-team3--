package com.st3.uber.dto.user.passenger;

public record PassengerProfileResponse(
  Long id,
  String email,
  String firstName,
  String lastName,
  String phoneNumber,
  String address
) {}
