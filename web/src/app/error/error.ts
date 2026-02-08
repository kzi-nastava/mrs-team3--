import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-error',
  templateUrl: './error.html',
  styleUrls: ['./error.css']
})
export class ErrorComponent {

  constructor(private router: Router, private authService: AuthService) {}

  goHome(): void {
    const userRole = this.authService.getUserRole(); // Assume this method retrieves the user's role
    if (userRole === 'ADMIN' || userRole === 'DRIVER') {
      this.router.navigate(['/profile']);
    } else {
    this.router.navigate(['/']);
  }

  
  }
}
