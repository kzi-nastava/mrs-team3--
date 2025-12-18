import { Component } from '@angular/core';
import { AuthService } from '../services/auth.service';
import {ButtonModule} from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SplitterModule } from 'primeng/splitter';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule} from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [FormsModule, ButtonModule, CardModule, SplitterModule, InputTextModule],
})
export class Login {
  email: string = '';
  password: string = '';

  constructor(private authService: AuthService) {
  }

  onSubmit() {

    this.authService.login(this.email, this.password).subscribe({
      next: (res) => {
        console.log('LOGIN SUCCESS', res);
        alert("Login successful!");
        // TODO: cuvanje tokena i redirect
      },
      error: (err) => {
        console.error('LOGIN ERROR', err);
        alert('Invalid credentials');
      }
    });
  }
}
