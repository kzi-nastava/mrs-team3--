import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-driver-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-sidebar.html',
  styleUrl: './driver-sidebar.css'
})
export class DriverSidebarComponent {

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goDriverDashboard() {
    // Navigate to driver dashboard when you create it
    // this.router.navigate(['/driver-dashboard']);
  }

  goHistory() {
    this.router.navigate(['/ride-history']);
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
}