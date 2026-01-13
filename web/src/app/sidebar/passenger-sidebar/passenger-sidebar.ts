import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-passenger-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-sidebar.html',
  styleUrl: './passenger-sidebar.css'
})
export class PassengerSidebarComponent {

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goBookRide() {
    // Navigate to book ride page when you create it
    this.router.navigate(['/book-ride']);
  }

  goHistory() {
    this.router.navigate(['/ride-history']);
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