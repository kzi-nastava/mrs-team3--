import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DriverService } from '../../services/driver.service';
import { MessageService } from 'primeng/api';
import { NotificationService } from '../../services/notification.service';
import { ChatService } from '../../services/chat.service';
import { LogoutModalComponent } from '../../logout-modal/logout-modal';
import { ChatPopupComponent } from '../../chat/chat';
import { Subject, takeUntil } from 'rxjs';
import {DriverStatusComponent} from '../../driver-dashboard/driver-status';

@Component({
  selector: 'app-driver-sidebar',
  standalone: true,
  imports: [LogoutModalComponent, ChatPopupComponent],
  templateUrl: './driver-sidebar.html',
  styleUrls: ['./driver-sidebar.css']
})
export class DriverSidebarComponent implements OnInit, OnDestroy {

  @ViewChild(ChatPopupComponent) chatPopup!: ChatPopupComponent;

  unreadCount = 0;
  showLogoutModal = false;
  isActive = true;
  inRide = false;

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private driverService: DriverService,
    private notificationService: NotificationService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef,
    private driverStatusStore: DriverStatusComponent
  ) { }

  ngOnInit(): void {
    this.driverStatusStore.inRide$
      .pipe(takeUntil(this.destroy$))
      .subscribe(v => {
        this.inRide = v;
        this.cdr.detectChanges();
      });

    this.driverStatusStore.active$
      .pipe(takeUntil(this.destroy$))
      .subscribe(v => {
        this.isActive = v;
        this.cdr.detectChanges();
      });


    setTimeout(() => {
      this.notificationService.unreadCount$
        .pipe(takeUntil(this.destroy$))
        .subscribe(count => {
          this.unreadCount = count;
          this.cdr.detectChanges();
        });
    }, 0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goDriverDashboard() {
    this.router.navigate(['/driver-dashboard']);
  }

  goNotifications() {
    this.router.navigate(['/driver-notifications']);
  }

  goHistory() {
    this.router.navigate(['/driver-history']);
  }

  goEarnings() {
    alert('Earnings - to be implemented');
  }

  goMessages() {
    if (this.chatPopup) {
      this.chatPopup.toggle();
    } else {
      console.warn('Chat popup not yet initialized');
    }
  }

  goProfile() {
    this.router.navigate(['/profile']);
  }

  logout() {
    this.showLogoutModal = true;
  }

  onLogoutConfirm() {
    this.showLogoutModal = false;
    this.authService.logout();
  }

  onLogoutCancel() {
    this.showLogoutModal = false;
  }

  goReport() {
  this.router.navigate(['/report']);
}

  isChangingStatus = false;

  toggleActiveStatus(): void {
    if (this.isChangingStatus) return;

    this.isChangingStatus = true;

    this.driverService.setActiveStatus().subscribe({
      next: (res) => {
        this.driverStatusStore.setActive(res.active);

        if (res.activityRequest) {
          this.messageService.add({
            severity: 'info',
            summary: 'Request queued',
            detail: 'Status will change after finishing the current ride.'
          });
        } else {
          this.messageService.add({
            severity: 'success',
            summary: 'Status updated',
            detail: res.active
              ? 'You are now active.'
              : 'You are now inactive.'
          });
        }
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Status Change Failed',
          detail: err.error?.message || 'Unable to change status.'
        });
      },
      complete: () => {
        this.isChangingStatus = false;
      }
    });
  }

}
