import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { tap } from 'rxjs/operators';

import { env } from '../../env/env';
import { WebSocketService } from './websocket.service';
import {
  Notification,
  NotificationCount,
  NotificationType
} from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private apiUrl = `${env.API_URL}/api/notifications`;

  // ===================== STATE =====================
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  readonly notifications$ = this.notificationsSubject.asObservable();
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  private isInitialized = false;
  private wsSubscription?: Subscription;

  constructor(
    private http: HttpClient,
    private websocketService: WebSocketService,
    private ngZone: NgZone
  ) {}

  // =====================================================
  // INITIALIZATION (CALL ONLY AFTER LOGIN)
  // =====================================================
  initialize(token: string): void {
    if (this.isInitialized) return;
    if (!token) return;

    // 1️⃣ Connect WebSocket with token
    this.websocketService.connect(token);

    // 2️⃣ Subscribe ONCE to WS notifications
    this.wsSubscription = this.websocketService.notifications$
      .subscribe(notification => {
        if (!notification) return;

        this.ngZone.run(() => {
          this.addNewNotification(notification);
        });
      });

    // 3️⃣ Load initial HTTP state
    this.loadNotifications();
    this.loadUnreadCount();

    this.isInitialized = true;
  }

  // =====================================================
  // HTTP LOADERS
  // =====================================================
  loadNotifications(): void {
    this.http.get<Notification[]>(this.apiUrl).subscribe({
      next: notifications => {
        this.notificationsSubject.next([...notifications]);
      }
    });
  }

  loadUnreadCount(): void {
    this.http
      .get<NotificationCount>(`${this.apiUrl}/unread/count`)
      .subscribe({
        next: result => {
          this.unreadCountSubject.next(result.count);
        }
      });
  }

  // =====================================================
  // MUTATIONS
  // =====================================================
  markAsRead(notificationId: number): Observable<Notification> {
    return this.http
      .put<Notification>(`${this.apiUrl}/${notificationId}/read`, {})
      .pipe(
        tap(() => {
          const updated = this.notificationsSubject.value.map(n =>
            n.id === notificationId ? { ...n, isRead: true } : n
          );
          this.notificationsSubject.next(updated);
          this.updateUnreadCount();
        })
      );
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        const updated = this.notificationsSubject.value.map(n => ({
          ...n,
          isRead: true
        }));
        this.notificationsSubject.next(updated);
        this.unreadCountSubject.next(0);
      })
    );
  }

  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${notificationId}`).pipe(
      tap(() => {
        const filtered = this.notificationsSubject.value.filter(
          n => n.id !== notificationId
        );
        this.notificationsSubject.next(filtered);
        this.updateUnreadCount();
      })
    );
  }

  // =====================================================
  // INTERNAL HELPERS
  // =====================================================
  private addNewNotification(notification: Notification): void {
    const exists = this.notificationsSubject.value.some(
      n => n.id === notification.id
    );
    if (exists) return;

    this.notificationsSubject.next([
      notification,
      ...this.notificationsSubject.value
    ]);

    if (!notification.isRead) {
      this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
    }
  }

  private updateUnreadCount(): void {
    const count = this.notificationsSubject.value.filter(n => !n.isRead).length;
    this.unreadCountSubject.next(count);
  }

  // =====================================================
  // LOGOUT CLEANUP
  // =====================================================
  disconnect(): void {
    this.wsSubscription?.unsubscribe();
    this.wsSubscription = undefined;

    this.websocketService.disconnect();

    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);

    this.isInitialized = false;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  // =====================================================
  // FILTERING (USED BY COMPONENTS)
  // =====================================================
  filterNotificationsByRole(
    role: string,
    notifications: Notification[]
  ): Notification[] {

    const map: Record<string, NotificationType[]> = {
      ADMIN: [NotificationType.PANIC],
      PASSENGER: [
        NotificationType.ACCEPTED_RIDE,
        NotificationType.DECLINED_RIDE,
        NotificationType.RIDE_REMINDER,
        NotificationType.FINISHED_RIDE,
        NotificationType.RIDE_CANCELED,
        NotificationType.PROFILE_CHANGE
      ],
      DRIVER: [
        NotificationType.RIDE_REMINDER,
        NotificationType.RIDE_CANCELED,
        NotificationType.PROFILE_CHANGE,
        NotificationType.FINISHED_RIDE
      ]
    };

    return notifications.filter(n => map[role]?.includes(n.type));
  }
}
