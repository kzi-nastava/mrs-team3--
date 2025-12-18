import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../services/auth.service';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SplitterModule } from 'primeng/splitter';
import { InputTextModule } from 'primeng/inputtext';

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

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  goRegister() {
    this.router.navigateByUrl('/register');
  }

  onSubmit() {
    // basic guard
    if (!this.email.trim() || !this.password) {
      alert('Please enter email and password.');
      return;
    }

    this.authService.login(this.email.trim(), this.password).subscribe({
      next: (res) => {
        console.log('LOGIN SUCCESS', res);
        alert('Login successful!');
        // TODO: cuvanje tokena i redirect
        // npr: this.router.navigateByUrl('/');
      },
      error: (err) => {
        console.error('LOGIN ERROR', err);
        alert('Invalid credentials');
      },
    });
  }
}
