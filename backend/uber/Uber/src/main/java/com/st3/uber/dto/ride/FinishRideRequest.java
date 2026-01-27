package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;

public record FinishRideRequest(
        Location actualEndLocation // null means use planned end
        // actualStops are tracked separately via reachStop() calls
) {}