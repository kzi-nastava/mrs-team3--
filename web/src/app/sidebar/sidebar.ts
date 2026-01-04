import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent {

  role: 'DRIVER' | 'ADMIN' | 'GUEST' | 'REGISTERED' = 'DRIVER';
  // kasnije: role = authService.getRole();

  constructor(private router: Router) {}


  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get isNotGuest(): boolean {
    return this.role !== 'GUEST';
  }


  goHome() {
    this.router.navigateByUrl('/');
  }

  goHistory() {
    this.router.navigateByUrl('/ride-history');
  }

  goMessages() {
    alert('Messages - to be implemented');
  }

  goProfile() {
    this.router.navigateByUrl('/profile');
  }

  goLogin() {
    this.router.navigateByUrl('/login');
  }

  goDriverRegister() {
    this.router.navigate(['/driver-register']);
  }
}
