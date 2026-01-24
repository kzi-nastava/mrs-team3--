package com.st3.uber.dto.route;

import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.enums.VehicleType;
import java.util.List;

public record FavoriteRouteRequest(
        Long id,
        Long rideId,
        LocationRequest from,
        LocationRequest to,
        List<LocationRequest> stops,
        VehicleType vehicleType,
        boolean babyTransport,
        boolean petTransport
) {}
