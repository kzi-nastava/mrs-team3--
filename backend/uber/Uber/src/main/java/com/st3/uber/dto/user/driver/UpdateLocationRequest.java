package com.st3.uber.dto.user.driver;

public record UpdateLocationRequest(
        double latitude,
        double longitude
) {}