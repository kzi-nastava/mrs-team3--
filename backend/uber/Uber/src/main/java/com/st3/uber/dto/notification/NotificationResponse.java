package com.st3.uber.dto.notification;

import com.st3.uber.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        NotificationType type,
        Boolean isRead,
        LocalDateTime createdAt,
        Long relatedEntityId
) {}