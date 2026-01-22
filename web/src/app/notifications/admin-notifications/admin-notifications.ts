import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationType } from '../../models/notification.model';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: 'admin-notifications.html',
  styleUrls: ['admin-notifications.css'],
})

    
export class AdminNotificationsComponent implements OnInit, OnDestroy {
  allNotifications: Notification[] = [];
  filteredNotifications: Notification[] = [];
  unreadCount = 0;
  filter: 'all' | 'unread' = 'all';
  
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
        this.allNotifications = this.notificationService.filterNotificationsByRole('ADMIN', notifications);
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

  setFilter(filter: 'all' | 'unread'): void {
    this.filter = filter;
    this.applyFilter();
  }

  applyFilter(): void {
    let filtered = [...this.allNotifications];
    if (this.filter === 'unread') {
      filtered = filtered.filter(n => !n.isRead);
    } 
    
    this.filteredNotifications = filtered;
  }

  handleNotificationClick(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe();
    }

    // if (notification.relatedEntityId) {
    //   // For panic notifications, navigate to ride details or panic dashboard
    //   if (notification.type === NotificationType.PANIC) {
        
    //   }
    // }
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
      [NotificationType.PANIC]: '🚨',
      [NotificationType.PROFILE_CHANGE]: '👤',
      [NotificationType.ACCEPTED_RIDE]: '✅',
      [NotificationType.DECLINED_RIDE]: '❌',
      [NotificationType.RIDE_REMINDER]: '⏰',
      [NotificationType.FINISHED_RIDE]: '🏁',
      [NotificationType.RIDE_CANCELED]: '🚫'
    };
    return icons[type] || '🔔';
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