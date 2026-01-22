package com.st3.uber.dto.notification;

public record MarkMultipleAsReadRequest(
        java.util.List<Long> notificationIds
) {}