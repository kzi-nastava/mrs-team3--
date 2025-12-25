import { Component } from '@angular/core';
import {MessageService} from 'primeng/api';
import { AuthService } from '../services/auth.service';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Splitter} from 'primeng/splitter';
import {ToastModule} from 'primeng/toast';

@Component({
  selector: 'app-forgot-password',
  imports: [
    FormsModule,
    InputText,
    Button,
    Card,
    Splitter,
    ToastModule
  ],
  templateUrl: './forgot-password.html',
  styleUrl: '../change-password/change-password.css',
  standalone: true,
  providers: [MessageService],
})

export class ForgotPassword {
  email: string = '';

  constructor(
    private authService: AuthService,
    private messageService: MessageService
  ) {}
  onSubmit() {
    if (!this.email.trim()) {
      console.log('Please enter email.');
      return;
    }

    this.authService.forgotPassword(this.email.trim()).subscribe({
      next: (res) => {
        console.log('FORGOT PASSWORD SUCCESS', res);
        this.messageService.add({severity:'success', summary: 'Email Sent', detail: 'Please check your email for password reset instructions.'});
      },
      error: (err) => {
        console.error('FORGOT PASSWORD ERROR', err);
        this.messageService.add({severity:'error', summary: 'Request Failed', detail: 'Unable to process your request at this time.'});
      },
    });
  }
}
