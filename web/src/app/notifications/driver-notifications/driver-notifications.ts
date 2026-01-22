import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationType } from '../../models/notification.model';

@Component({
  selector: 'app-driver-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: 'driver-notifications.html',
  styleUrls: ['driver-notifications.css']

})
export class DriverNotificationsComponent implements OnInit, OnDestroy {
  allNotifications: Notification[] = [];
  filteredNotifications: Notification[] = [];
  unreadCount = 0;
  filter: 'all' | 'unread' | 'rides' = 'all';
  
  private destroy$ = new Subject<void>();

  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.notificationService.notifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notifications => {
        this.allNotifications = this.notificationService.filterNotificationsByRole('DRIVER', notifications);
        this.applyFilter();
      });

    this.notificationService.unreadCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.unreadCount = count;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setFilter(filter: 'all' | 'unread' | 'rides'): void {
    this.filter = filter;
    this.applyFilter();
  }

  applyFilter(): void {
    let filtered = [...this.allNotifications];

    if (this.filter === 'unread') {
      filtered = filtered.filter(n => !n.isRead);
    } else if (this.filter === 'rides') {
      filtered = filtered.filter(n => 
        n.type === NotificationType.RIDE_REMINDER ||
        n.type === NotificationType.RIDE_CANCELED ||
        n.type === NotificationType.FINISHED_RIDE
      );
    }

    this.filteredNotifications = filtered;
  }

  handleNotificationClick(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe();
    }

    // Navigate based on notification type
    if (notification.relatedEntityId) {
      if (notification.type === NotificationType.RIDE_REMINDER ||
          notification.type === NotificationType.FINISHED_RIDE ||
          notification.type === NotificationType.RIDE_CANCELED) {
        // Navigate to ride details
        this.router.navigate(['/driver-history']);
      }
    }
    if (notification.type === NotificationType.PROFILE_CHANGE) {
      this.router.navigate(['/profile']);
    }
  }

  markAsRead(event: Event, notificationId: number): void {
    event.stopPropagation();
    this.notificationService.markAsRead(notificationId).subscribe();
  }

  deleteNotification(event: Event, notificationId: number): void {
    event.stopPropagation();
    if (confirm('Delete this notification?')) {
      this.notificationService.deleteNotification(notificationId).subscribe();
    }
  }

  markAllAsRead(): void {
    if (confirm('Mark all notifications as read?')) {
      this.notificationService.markAllAsRead().subscribe();
    }
  }

  getNotificationIcon(type: NotificationType): string {
    const icons: { [key in NotificationType]: string } = {
      [NotificationType.RIDE_REMINDER]: '⏰',
      [NotificationType.RIDE_CANCELED]: '🚫',
      [NotificationType.PROFILE_CHANGE]: '👤',
      [NotificationType.FINISHED_RIDE]: '🏁',
      [NotificationType.PANIC]: '🚨',
      [NotificationType.ACCEPTED_RIDE]: '✅',
      [NotificationType.DECLINED_RIDE]: '❌'
    };
    return icons[type] || '🔔';
  }

  getNotificationColor(type: NotificationType): string {
    const colors: { [key in NotificationType]: string } = {
      [NotificationType.RIDE_REMINDER]: 'var(--color-orange)',
      [NotificationType.RIDE_CANCELED]: 'var(--color-red)',
      [NotificationType.PROFILE_CHANGE]: 'var(--color-primary)',
      [NotificationType.FINISHED_RIDE]: 'var(--color-green)',
      [NotificationType.PANIC]: 'var(--color-red)',
      [NotificationType.ACCEPTED_RIDE]: 'var(--color-green)',
      [NotificationType.DECLINED_RIDE]: 'var(--color-red)'
    };
    return colors[type] || 'var(--color-gray)';
  }

  getNotificationTypeLabel(type: NotificationType): string {
    return type.replace(/_/g, ' ');
  }

  formatTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  }
}