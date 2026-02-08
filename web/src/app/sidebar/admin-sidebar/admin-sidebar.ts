import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { LogoutModalComponent } from '../../logout-modal/logout-modal';
import { Subject, takeUntil } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { ChatPopupComponent } from '../../chat/chat';



@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [LogoutModalComponent, ChatPopupComponent],
  templateUrl: './admin-sidebar.html',
  styleUrls: ['./admin-sidebar.css']
})
export class AdminSidebarComponent implements OnInit, OnDestroy {

  @ViewChild(ChatPopupComponent) chatPopup!: ChatPopupComponent;
  unreadCount = 0;
  showLogoutModal = false;
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private notificationService: NotificationService,
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

  goHome() {
    this.router.navigate(['/admin-history']);
  }

  goNotifications() {
    this.router.navigate(['/admin-notifications']);
  }

  registerDriver() {
    this.router.navigate(['/driver-register']);
  }

  goProfile() {
    this.router.navigate(['/profile']);
  }
  goPricing() {
    this.router.navigate(['/pricing-management']);
  }
  goMessages() {
    this.chatPopup.toggle();
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

  goProfileChangeRequests() {
    this.router.navigate(['/admin/profile-change-requests']);
  }

  goReport() {
    this.router.navigate(['/report']);
  }

}
