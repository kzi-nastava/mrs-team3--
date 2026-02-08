package com.st3.uber.dto.route;

import com.st3.uber.dto.location.LocationResponse;
import com.st3.uber.enums.VehicleType;
import java.util.List;

public record FavoriteRouteResponse(
        Long id,
        Long rideId,
        LocationResponse from,
        LocationResponse to,
        List<LocationResponse> stops,
        VehicleType vehicleType,
        boolean babyTransport,
        boolean petTransport
) {}
