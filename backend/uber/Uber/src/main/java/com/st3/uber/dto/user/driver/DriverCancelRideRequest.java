package com.st3.uber.dto.user.driver;

import lombok.Data;

@Data
public class DriverCancelRideRequest {
  private String cancellationReason;
}
