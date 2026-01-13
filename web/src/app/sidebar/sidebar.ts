import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { GuestSidebarComponent } from './guest-sidebar/guest-sidebar';
import { PassengerSidebarComponent } from './passenger-sidebar/passenger-sidebar';
import { DriverSidebarComponent } from './driver-sidebar/driver-sidebar';
import { AdminSidebarComponent } from './admin-sidebar/admin-sidebar';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    GuestSidebarComponent,
    PassengerSidebarComponent,
    DriverSidebarComponent,
    AdminSidebarComponent
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit, OnDestroy {
  userRole: string | null = null;
  private userSubscription?: Subscription;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    // Subscribe to user changes
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.userRole = user?.role || null;
    });
  }

  ngOnDestroy() {
    this.userSubscription?.unsubscribe();
  }

  get isGuest(): boolean {
    return !this.authService.isAuthenticated();
  }

  get isPassenger(): boolean {
    return this.authService.isPassenger();
  }

  get isDriver(): boolean {
    return this.authService.isDriver();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}