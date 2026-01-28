package com.st3.uber.dto.ride;

import com.st3.uber.domain.Location;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class IncomingRideResponse {
    private Long id;
    private Location startLocation;
    private Location endLocation;
    private List<Location> stops;
    private LocalDateTime startTime;
}
