package com.st3.uber.dto.user.driver;

import lombok.Data;

@Data
public class DriverActivityResponse {
  private Long driverId;
  private Boolean isActive;

  public DriverActivityResponse(Long id, Boolean isActive) {
    this.driverId = id;
    this.isActive = isActive;
  }
}
