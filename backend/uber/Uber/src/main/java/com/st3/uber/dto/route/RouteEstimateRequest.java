package com.st3.uber.dto.route;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.enums.VehicleType;
import lombok.Data;

import java.util.List;

@Data
public class RouteEstimateRequest {

    private LocationRequest startLocation;
    private LocationRequest endLocation;

    private List<LocationRequest> stops;
    private VehicleType vehicleType;
}
