package com.st3.uber.dto.vehicle;

import com.st3.uber.enums.VehicleType;
import java.time.LocalDateTime;

// 2.1.1 - Response for active vehicles on map
public record ActiveVehicleResponse(
        Long vehicleId,
        double latitude,
        double longitude,
        String currentAddress,
        boolean isBusy,
        VehicleType type,
        String registrationNumber,
        LocalDateTime locationUpdatedAt
) {}