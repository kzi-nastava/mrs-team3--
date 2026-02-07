import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Subject, takeUntil } from 'rxjs';
import { NotificationService } from '../../services/notification.service';
import { LogoutModalComponent } from '../../logout-modal/logout-modal';
import { ChatService } from '../../services/chat.service';
import { ChatPopupComponent } from '../../chat/chat';

@Component({
  selector: 'app-passenger-sidebar',
  standalone: true,
  imports: [LogoutModalComponent, ChatPopupComponent],
  templateUrl: './passenger-sidebar.html',
  styleUrls: ['./passenger-sidebar.css']
})
export class PassengerSidebarComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  showLogoutModal = false;
  private destroy$ = new Subject<void>();
  @ViewChild(ChatPopupComponent) chatPopup!: ChatPopupComponent;


  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

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
    this.router.navigate(['/']);
  }

  goTrackingRide() {
    this.router.navigate(['/ride-tracking']);
  }

  goBookRide() {
    this.router.navigate(['/book-ride']);
  }

  goHistory() {
    this.router.navigate(['/passenger-history']);
  }

  goNotifications() {
    this.router.navigate(['/passenger-notifications']);
  }


  goMessages() {
    this.chatPopup.toggle();
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

  incomingRides() {
    this.router.navigate(['/incoming-rides']);
  }
}
