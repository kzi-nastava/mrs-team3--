package com.st3.uber.controller;

import com.st3.uber.dto.notification.MarkMultipleAsReadRequest;
import com.st3.uber.dto.notification.NotificationResponse;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/notifications
     * Get all notifications for the authenticated user
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread
     * Get only unread notifications for the authenticated user
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @GetMapping(value = "/unread", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread/count
     * Get count of unread notifications
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @GetMapping(value = "/unread/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /api/notifications/type/{type}
     * Get notifications by type for the authenticated user
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @GetMapping(value = "/type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NotificationResponse>> getNotificationsByType(
            Authentication authentication,
            @PathVariable NotificationType type
    ) {
        Long userId = extractUserId(authentication);
        List<NotificationResponse> notifications = notificationService.getNotificationsByType(userId, type);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a single notification as read
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @PutMapping(value = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        // Optional: Add check to ensure notification belongs to authenticated user
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/notifications/read
     * Mark multiple notifications as read
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @PutMapping(
            value = "/read",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> markMultipleAsRead(
            @RequestBody MarkMultipleAsReadRequest request,
            Authentication authentication
    ) {
        notificationService.markMultipleAsRead(request.notificationIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read for the authenticated user
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/notifications/{id}
     * Delete a notification
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication
    ) {
        // Optional: Add check to ensure notification belongs to authenticated user
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/notifications/cleanup
     * Cleanup old read notifications (older than 30 days)
     */
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','DRIVER')")
    @DeleteMapping("/cleanup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> cleanupOldNotifications(
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        notificationService.cleanupOldNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    // Helper method to extract user ID from JWT token
    private Long extractUserId(Authentication authentication) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        return jwt.getToken().getClaim("uid");
    }
}