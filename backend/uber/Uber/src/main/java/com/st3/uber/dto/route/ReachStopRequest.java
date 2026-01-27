package com.st3.uber.dto.route;

public record ReachStopRequest(
        int stopIndex // 0-based index of the stop in the stops list
) {}
