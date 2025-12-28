package com.st3.uber.dto.ride;

import java.time.LocalDateTime;

// POST /api/rides/{id}/report-inconsistency - Response
public record ReportInconsistencyResponse(
        Long reportId,
        Long rideId,
        String message,
        LocalDateTime reportedAt
) {}