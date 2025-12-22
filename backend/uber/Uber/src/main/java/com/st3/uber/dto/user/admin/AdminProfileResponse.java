package com.st3.uber.dto.user.admin;

public record AdminProfileResponse(
  Long id,
  String email,
  String firstName,
  String lastName
) {}
