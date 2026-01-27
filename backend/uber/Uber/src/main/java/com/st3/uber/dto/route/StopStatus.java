package com.st3.uber.dto.route;

import com.st3.uber.domain.Location;

import java.time.LocalDateTime;

public record StopStatus(
        int stopIndex,
        Location location,
        boolean reached,
        LocalDateTime reachedAt
) {}

