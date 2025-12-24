package com.st3.uber.dto.user;

public record UpdateUserProfileRequest(
  String firstName,
  String lastName,
  String phoneNumber,
  String address
) {}
