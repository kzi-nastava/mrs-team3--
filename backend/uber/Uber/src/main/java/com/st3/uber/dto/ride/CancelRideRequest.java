package com.st3.uber.dto.ride;

import com.st3.uber.enums.CancelledBy;
import lombok.Data;

@Data
public class CancelRideRequest {
    CancelledBy cancelledBy;
    String cancellationReason;
}
