import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../services/auth.service';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SplitterModule } from 'primeng/splitter';
import { InputTextModule } from 'primeng/inputtext';
import {MessageService} from 'primeng/api';

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
    private router: Router,
    private messageService: MessageService
  ) {}

  goRegister() {
    this.router.navigateByUrl('/register');
  }

  onSubmit() {
    if (!this.email.trim() || !this.password) {
      console.log('Please enter email and password.');
      return;
    }

    this.authService.login(this.email.trim(), this.password).subscribe({
      next: (res) => {
        console.log('LOGIN SUCCESS', res);
        this.messageService.add({severity:'success', summary: 'Login Successful', detail: 'Welcome back!'});
      },
      error: (err) => {
        console.error('LOGIN ERROR', err);
        this.messageService.add({severity:'error', summary: 'Login Failed', detail: 'Invalid email or password.'});

      },
    });
  }

  changePassword() {
    this.router.navigate(['/forgot-password']).then();
  }
}
