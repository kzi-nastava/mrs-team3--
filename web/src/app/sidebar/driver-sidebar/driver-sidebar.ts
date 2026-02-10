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
  
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private driverService: DriverService,
    private notificationService: NotificationService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    // Subscribe to notification count
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


  toggleActiveStatus(): void {
    const prev = this.isActive;
    this.isActive = !this.isActive;
    this.driverService.setActiveStatus().subscribe({
      next: () => {
      },
      error: (err) => {
        this.isActive = prev;
        this.messageService.add({
          severity: 'error',
          summary: 'Status Change Failed',
          detail: err.error?.message || 'Unable to change status.'
        });
      }
    });
  }
}