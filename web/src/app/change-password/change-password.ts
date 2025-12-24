import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ToastModule } from 'primeng/toast';
import {SplitterModule} from 'primeng/splitter';

@Component({
  selector: 'app-change-password',
  standalone: true,
  templateUrl: './change-password.html',
  styleUrls: ['../login/login.css'],
  providers: [MessageService],
  imports: [
    FormsModule,
    CardModule,
    SplitterModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    ToastModule
  ]
})
export class ChangePasswordComponent {
  email = '';
  newPassword = '';
  confirmPassword = '';

  strongRegex = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$';

  constructor(private router: Router, private messageService: MessageService) {}

  onSubmit() {
    if (this.newPassword !== this.confirmPassword) {
      this.messageService.add({
        severity: 'error',
        summary: 'Mismatch',
        detail: 'Passwords do not match.',
      });
      return;
    }

    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Password updated successfully.',
    });

  }
}
