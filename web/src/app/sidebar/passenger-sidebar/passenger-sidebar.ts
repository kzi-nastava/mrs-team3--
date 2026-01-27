import { Component, OnInit, OnDestroy,ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Subject, takeUntil } from 'rxjs';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-passenger-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-sidebar.html',
  styleUrls: ['./passenger-sidebar.css']
})
export class PassengerSidebarComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  private destroy$ = new Subject<void>();
    
  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private authService: AuthService,
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

  goHome() {
    this.router.navigate(['/']);
  }

  goTrackingRide() {
    this.router.navigate(['/ride-tracking']);
  }

  goBookRide() {
    // Navigate to book ride page when you create it
    this.router.navigate(['/book-ride']);
  }

  goHistory() {
    this.router.navigate(['/passenger-history']);
  }

  goNotifications() {
    this.router.navigate(['/passenger-notifications']);
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
}