import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../services/auth.service';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SplitterModule } from 'primeng/splitter';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [FormsModule, ButtonModule, CardModule, SplitterModule, InputTextModule],
})
export class Login {
  email = '';
  password = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  goRegister() {
    this.router.navigateByUrl('/register');
  }

  onSubmit() {
    if (!this.email.trim() || !this.password) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Please enter email and password.'
      });
      return;
    }

    this.loading = true;

    this.authService.login(this.email.trim(), this.password).subscribe({
      next: (res) => {
        console.log('LOGIN SUCCESS', res);
        console.log('User ID:', this.authService.getUserId());
        console.log('User Role:', this.authService.getUserRole());
        console.log('User Email:', this.authService.getUserEmail());

        this.messageService.add({
          severity: 'success',
          summary: 'Login Successful',
          detail: `Welcome back, ${res.email}!`
        });

        // Navigate based on role
        const role = this.authService.getUserRole();

        if (role === 'PASSENGER'|| role === 'ADMIN') {
          this.router.navigate(['/']);
        } else if (role === 'DRIVER') {
          this.router.navigate(['/profile']);
        }


        this.loading = false;
      },
      error: (err) => {
        console.error('LOGIN ERROR', err);

        let errorMessage = 'Invalid email or password.';

        // Handle specific error messages from backend
        if (err.error) {
          if (typeof err.error === 'string') {
            errorMessage = err.error;
          } else if (err.error.message) {
            errorMessage = err.error.message;
          }
        }

        // Check for specific error types
        if (errorMessage.includes('blocked')) {
          this.messageService.add({
            severity: 'error',
            summary: 'Account Blocked',
            detail: 'Your account has been blocked. Please contact support.'
          });
        } else if (errorMessage.includes('not verified')) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Email Not Verified',
            detail: 'Please verify your email before logging in.'
          });
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Login Failed',
            detail: errorMessage
          });
        }

        this.loading = false;
      },
    });
  }

  changePassword() {
    this.router.navigate(['/forgot-password']);
  }
}
