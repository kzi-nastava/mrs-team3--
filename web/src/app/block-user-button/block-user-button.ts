import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import { UserService } from '../services/user.service';

@Component({
  selector: 'app-block-user-button',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    ButtonModule
  ],
  templateUrl: './block-user-button.html',
})
export class BlockUserButtonComponent {

  @Input() userId!: number;
  @Input() isBlocked!: boolean;

  @Output() blockChanged = new EventEmitter<void>();

  showDialog = false;
  loading = false;
  reason = '';

  constructor(
    private userService: UserService,
    private messageService: MessageService
  ) {}

  openDialog() {
    this.reason = '';

    if (this.isBlocked) {
      this.toggleBlock();
    } else {
      this.showDialog = true;
    }
  }

  confirmBlock() {
    if (!this.reason.trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Reason required',
        detail: 'Please enter a reason'
      });
      return;
    }

    this.toggleBlock();
    this.showDialog = false;
  }

  toggleBlock() {
    this.loading = true;

    this.userService
      .blockUser(
        this.userId,
        !this.isBlocked,
        this.isBlocked ? undefined : this.reason
      )
      .subscribe({
        next: () => {
          this.reason = '';
          this.loading = false;

          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: this.isBlocked ? 'User unblocked' : 'User blocked'
          });

          this.blockChanged.emit();
        },
        error: () => {
          this.loading = false;

          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Operation failed'
          });
        }
      });
  }
}
