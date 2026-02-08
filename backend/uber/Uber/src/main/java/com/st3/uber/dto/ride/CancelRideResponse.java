package com.st3.uber.dto.ride;

import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CancelRideResponse {
    Long rideId;
    RideStatus status;

    public CancelRideResponse(CancelRideRequest request, Long rideId) {
        this.rideId = rideId;
        this.status = request.getCancelledBy() == null || request.getCancelledBy() == CancelledBy.DRIVER ?
            RideStatus.CANCELLED_BY_DRIVER :
            RideStatus.CANCELLED_BY_PASSENGER;
    }
}
