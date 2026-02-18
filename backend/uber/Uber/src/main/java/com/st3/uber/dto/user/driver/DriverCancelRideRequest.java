package com.st3.uber.dto.user.driver;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverCancelRideRequest {
  @NotBlank(message = "Cancellation reason is required")
  private String cancellationReason;
}
