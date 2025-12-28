package com.st3.uber.dto.user.driver;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.dto.ride.ReportInconsistencyResponse;
import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import java.time.LocalDateTime;
import java.util.List;


// GET /api/drivers/{driverId}/rides/{rideId} - Response for detailed ride view
public record DriverRideDetailResponse(
        Long rideId,
        String startAddress,
        String endAddress,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        RideStatus status,
        double price,
        double distance,
        VehicleType vehicleType,
        boolean wasCancelled,
        CancelledBy cancelledBy,
        String terminationReason,
        boolean hadPanicEvent,
        List<String> passengerNames,
        List<LocationRequest> plannedStops,
        List<LocationRequest> actualStops,
        Integer driverRating,
        Integer vehicleRating,
        String reviewComment,
        List<ReportInconsistencyResponse> inconsistencyReports
) {}