package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import com.st3.uber.enums.RideStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PassengerRideSummaryResponse {
  private Long id;
  private RideStatus status;
  private Location startLocation;
  private Location endLocation;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private boolean favorite;

  public PassengerRideSummaryResponse(Long id, RideStatus status, Location startLocation, Location endLocation,
                                      LocalDateTime startedAt, LocalDateTime finishedAt, boolean favorite){
    this.id = id;
    this.status = status;
    this.startLocation = startLocation;
    this.endLocation = endLocation;
    this.startTime = startedAt;
    this.endTime = finishedAt;
    this.favorite = favorite;
  }

  public PassengerRideSummaryResponse() {}
}
