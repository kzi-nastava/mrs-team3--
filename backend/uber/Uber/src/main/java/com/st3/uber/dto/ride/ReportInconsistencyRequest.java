package com.st3.uber.dto.ride;

// POST /api/rides/{id}/report-inconsistency - Request
public record ReportInconsistencyRequest(
        String reportText
) {}