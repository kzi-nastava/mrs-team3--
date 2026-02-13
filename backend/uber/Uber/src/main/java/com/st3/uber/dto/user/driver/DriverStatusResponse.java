package com.st3.uber.dto.user.driver;

import lombok.Data;

@Data
public class DriverStatusResponse {
  boolean active;
  boolean activityRequest;
  boolean inRide;

  public DriverStatusResponse(boolean active, boolean activityRequest, boolean inRide) {
    this.active = active;
    this.activityRequest = activityRequest;
    this.inRide = inRide;
  }
}
