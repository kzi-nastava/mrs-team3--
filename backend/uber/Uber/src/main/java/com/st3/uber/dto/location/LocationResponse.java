package com.st3.uber.dto.location;

public record LocationResponse(
        double latitude,
        double longitude,
        String address
) {}
