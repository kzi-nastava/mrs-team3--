import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-guest-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './guest-sidebar.html',
  styleUrl: './guest-sidebar.css'
})
export class GuestSidebarComponent {

  constructor(private router: Router) {}

  goHome() {
    this.router.navigate(['/']);
  }

  goLogin() {
    this.router.navigate(['/login']);
  }

  goRegister() {
    this.router.navigate(['/register']);
  }
}