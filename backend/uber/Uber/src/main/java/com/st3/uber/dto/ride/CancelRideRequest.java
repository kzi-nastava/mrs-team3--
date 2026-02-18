package com.st3.uber.dto.ride;

import com.st3.uber.enums.CancelledBy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelRideRequest {
    @NotNull(message = "CancelledBy is required")
    CancelledBy cancelledBy;

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, max = 255, message = "Cancellation reason must be between 5 and 255 characters")
    String cancellationReason;
}
