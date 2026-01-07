package com.st3.uber.dto.route;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RouteEstimateResponse{
  private int estimatedTimeMinutes;
  public RouteEstimateResponse(int estimatedTimeMinutes){
    this.estimatedTimeMinutes = estimatedTimeMinutes;
  }
}