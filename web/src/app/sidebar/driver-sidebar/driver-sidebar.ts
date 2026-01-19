import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { DriverService } from '../../services/driver.service';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-driver-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-sidebar.html',
  styleUrls: ['./driver-sidebar.css']
})
export class DriverSidebarComponent {

  constructor(
    private router: Router,
    private authService: AuthService,
    private driverService: DriverService,
    private messageService: MessageService
  ) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goDriverDashboard() {
    // Navigate to driver dashboard when you create it
    // this.router.navigate(['/driver-dashboard']);
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

  isActive = false;

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
