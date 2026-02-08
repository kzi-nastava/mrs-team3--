import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ToastModule } from 'primeng/toast';
import { SplitterModule } from 'primeng/splitter';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  templateUrl: './reset-password.html',
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
export class ResetPasswordComponent {

  token = '';
  newPassword = '';
  confirmPassword = '';

  mode: 'reset' | 'activate' = 'reset';

  strongRegex = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$';

  constructor(
    private router: Router,
    private messageService: MessageService,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {
    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token') ?? '';
      this.mode = params.get('mode') === 'activate' ? 'activate' : 'reset';

      if (!this.token) {
        this.router.navigate(['/verification-result'], {
          queryParams: { status: 'invalid' }
        }).then(() => {});
      }
    });
  }

  onSubmit() {
    if (!this.token) {
      this.messageService.add({
        severity: 'error',
        summary: 'Missing token',
        detail: 'Reset token is missing. Please open the link from your email again.',
      });
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.messageService.add({
        severity: 'error',
        summary: 'Mismatch',
        detail: 'Passwords do not match.',
      });
      return;
    }

    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail:
            this.mode === 'activate'
              ? 'Account activated successfully. You can now log in.'
              : 'Password updated successfully.',
        });

        setTimeout(() => {
          this.router.navigate(['/verification-result'], {
            queryParams: { status: 'success' }
          });
        }, 1200);
      },
      error: (err) => {
        console.error('RESET PASSWORD ERROR', err);

        if ([400, 401, 404].includes(err.status)) {
          this.router.navigate(['/verification-result'], {
            queryParams: { status: 'invalid' }
          }).then(() => {});
          return;
        }

        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to reset password. Please try again.',
        });
      },
    });
  }
}
