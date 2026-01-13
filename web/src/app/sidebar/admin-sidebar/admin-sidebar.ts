import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-sidebar.html',
  styleUrl: './admin-sidebar.css'
})
export class AdminSidebarComponent {

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goAdminDashboard() {
    // Navigate to admin dashboard when you create it
    // this.router.navigate(['/admin']);
  }

  goManageUsers() {
    // Navigate to user management when implemented
    // this.router.navigate(['/admin/users']);
  }

  goManageRides() {
    // Navigate to ride management when implemented
    // this.router.navigate(['/admin/rides']);
  }

  goReports() {
    // Navigate to reports when implemented
    // alert('Reports - to be implemented');
  }

  goSettings() {
    // Navigate to admin settings when implemented
    // alert('Settings - to be implemented');
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