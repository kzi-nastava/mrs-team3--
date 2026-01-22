package com.st3.uber.service;

import com.st3.uber.domain.Notification;
import com.st3.uber.domain.User;
import com.st3.uber.dto.notification.CreateNotificationRequest;
import com.st3.uber.dto.notification.NotificationResponse;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.repository.NotificationRepository;
import com.st3.uber.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Create and send a notification to a user
     * This also sends it via WebSocket for real-time delivery
     */
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        System.out.println("=== Creating Notification ===");
        System.out.println("User ID: " + request.userId());
        System.out.println("Message: " + request.message());
        System.out.println("Type: " + request.type());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setRelatedEntityId(request.relatedEntityId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        System.out.println("✅ Notification saved to database with ID: " + saved.getId());

        // Send via WebSocket to user's personal channel
        NotificationResponse response = mapToResponse(saved);

        String destination = "/queue/notifications";
        String userIdString = user.getId().toString();

        System.out.println("📡 Sending WebSocket notification:");
        System.out.println("   User ID: " + userIdString);
        System.out.println("   Destination: " + destination);
        System.out.println("   Full path will be: /user/" + userIdString + destination);
        System.out.println("   Response: " + response);

        try {
            messagingTemplate.convertAndSendToUser(
                    userIdString,           // Must match the Principal name set in WebSocketAuthInterceptor
                    destination,
                    response
            );
            System.out.println("✅ WebSocket message sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Create notification without request DTO (for internal service use)
     */
    @Transactional
    public NotificationResponse createNotification(
            Long userId,
            String message,
            NotificationType type,
            Long relatedEntityId
    ) {
        CreateNotificationRequest request = new CreateNotificationRequest(
                userId, message, type, relatedEntityId
        );
        return createNotification(request);
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get only unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get notifications by type for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByType(Long userId, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(user, type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark a single notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);

        return mapToResponse(updated);
    }

    /**
     * Mark multiple notifications as read
     */
    @Transactional
    public void markMultipleAsRead(List<Long> notificationIds) {
        notificationIds.forEach(this::markAsRead);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        notificationRepository.markAllAsReadForUser(user);
    }

    /**
     * Get count of unread notifications
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Cleanup old read notifications (older than 30 days)
     */
    @Transactional
    public void cleanupOldNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldReadNotifications(user, thirtyDaysAgo);
    }

    // Helper method to map entity to DTO
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getRelatedEntityId()
        );
    }
}