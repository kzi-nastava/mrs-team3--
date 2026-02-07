import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DriverService } from '../../services/driver.service';
import { MessageService } from 'primeng/api';
import { NotificationService } from '../../services/notification.service';
import { LogoutModalComponent } from '../../logout-modal/logout-modal';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-driver-sidebar',
  standalone: true,
  imports: [LogoutModalComponent],
  templateUrl: './driver-sidebar.html',
  styleUrls: ['./driver-sidebar.css']
})
export class DriverSidebarComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  showLogoutModal = false;
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
    alert('Messages - to be implemented');
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


  isActive = true;

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