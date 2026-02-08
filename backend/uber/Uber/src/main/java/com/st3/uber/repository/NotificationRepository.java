package com.st3.uber.repository;

import com.st3.uber.domain.Notification;
import com.st3.uber.domain.User;
import com.st3.uber.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all notifications for a specific user, ordered by most recent first
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Find unread notifications for a user
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // Find notifications by user and type
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    // Find notifications created after a certain date
    List<Notification> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime date);

    // Count unread notifications for a user
    Long countByUserAndIsReadFalse(User user);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") User user);

    // Delete old read notifications (cleanup)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.createdAt < :date")
    void deleteOldReadNotifications(@Param("user") User user, @Param("date") LocalDateTime date);
}