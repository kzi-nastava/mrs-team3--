package com.st3.uber.dto.notification;

import com.st3.uber.enums.NotificationType;

public record CreateNotificationRequest(
        Long userId,
        String message,
        NotificationType type,
        Long relatedEntityId
) {}