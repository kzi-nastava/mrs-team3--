import { Component } from '@angular/core';
import {ReactiveFormsModule,  FormBuilder, Validators, FormGroup } from '@angular/forms';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports: [ReactiveFormsModule],
})
export class Login {
  form: FormGroup;

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      email: ['', Validators.required],
      password: ['', Validators.required],
    });
  }



  onSubmit() {
    if (this.form.invalid) return;
    // console.log('LOGIN', this.form.value);
    const { email, password } = this.form.value;

    this.authService.login(email, password).subscribe({
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
