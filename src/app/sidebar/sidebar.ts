import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgIf} from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent {

  // HARD-CODED for now (easy to replace later)
  role: 'DRIVER' | 'ADMIN' | 'GUEST' | 'REGISTERED' = 'DRIVER';
  // role = authService.getRole();
  constructor(private router: Router) {}

  goHome() {
    this.router.navigateByUrl('/');
  }

  goHistory() {
    this.router.navigateByUrl('/ride-history');
  }

  goMessages() {
    // this.router.navigateByUrl('/messages');
    alert('Messages - to be implemented');
  }

  goProfile() {
    this.router.navigateByUrl('/profile');
  }

  goLogin() {
    this.router.navigateByUrl('/login');
  }
}
