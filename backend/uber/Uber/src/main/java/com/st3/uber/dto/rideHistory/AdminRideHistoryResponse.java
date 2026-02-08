package com.st3.uber.dto.rideHistory;

import com.st3.uber.domain.Location;
import com.st3.uber.enums.RideStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
public class AdminRideHistoryResponse{
  private Long id;
  private RideStatus status;
  private Location startLocation;
  private Location endLocation;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private double price;
  private boolean panic;

  public AdminRideHistoryResponse(Long id, RideStatus status, Location startLocation, Location endLocation,
                                   LocalDateTime startTime, LocalDateTime endTime, double price, boolean panic) {
    this.id = id;
    this.status = status;
    this.startLocation = startLocation;
    this.endLocation = endLocation;
    this.startTime = startTime;
    this.endTime = endTime;
    this.price = price;
    this.panic = panic;
  }
  public AdminRideHistoryResponse() {
  }
}
