import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { DriverService } from '../../services/driver.service';
import { MessageService } from 'primeng/api';
import { NotificationService } from '../../services/notification.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-driver-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-sidebar.html',
  styleUrls: ['./driver-sidebar.css']
})
export class DriverSidebarComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private driverService: DriverService,
    private notificationService: NotificationService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef  
  ) {}

  ngOnInit(): void {
    // Use setTimeout to defer the subscription to avoid ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.notificationService.unreadCount$
        .pipe(takeUntil(this.destroy$))
        .subscribe(count => {
          this.unreadCount = count;
          this.cdr.detectChanges(); // Manually trigger change detection
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
    // Navigate to earnings page when implemented
    alert('Earnings - to be implemented');
  }

  goMessages() {
    // Navigate to messages when implemented
    alert('Messages - to be implemented');
  }

  goProfile() {
    this.router.navigate(['/profile']);
  }

  logout() {
    if (confirm('Are you sure you want to logout?')) {
      this.authService.logout();
    }
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
