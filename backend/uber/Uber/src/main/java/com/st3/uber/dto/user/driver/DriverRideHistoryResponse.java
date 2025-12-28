package com.st3.uber.dto.user.driver;

import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import java.time.LocalDateTime;
import java.util.List;

// GET /api/drivers/{id}/rides - Response for driver's ride history list
public record DriverRideHistoryResponse(
        Long rideId,
        String startAddress,
        String endAddress,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        RideStatus status,
        double price,
        double distance,
        boolean wasCancelled,
        CancelledBy cancelledBy,
        boolean hadPanicEvent,
        List<String> passengerNames
) {}