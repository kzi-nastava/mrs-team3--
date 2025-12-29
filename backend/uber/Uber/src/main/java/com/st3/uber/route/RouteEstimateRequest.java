package com.st3.uber.route;
import com.st3.uber.dto.location.LocationRequest;
import lombok.Data;

@Data
public class RouteEstimateRequest {
  LocationRequest startLocation;
  LocationRequest endLocation;
}