package com.st3.uber.dto.route;

import com.st3.uber.domain.Location;
import lombok.Data;

@Data
public class CalculateSimpleRouteRequest {
  private Location startLocation;
  private Location endLocation;
}
