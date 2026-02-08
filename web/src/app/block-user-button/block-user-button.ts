import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MessageService } from 'primeng/api';

import { UserService } from '../services/user.service';

@Component({
  selector: 'app-block-user-button',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './block-user-button.html',
  styleUrls: ['./block-user-button.css']
})
export class BlockUserButtonComponent {

  @Input() userId!: number;
  @Input() isBlocked!: boolean;

  @Output() blockChanged = new EventEmitter<void>();

  loading = false;
  showDialog = false;
  reason = '';

  constructor(
    private userService: UserService,
    private messageService: MessageService
  ) {}

  openDialog() {
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
        summary: 'Warning',
        detail: 'Reason is required'
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
          this.isBlocked = !this.isBlocked;
          this.reason = '';
          this.loading = false;

          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: this.isBlocked ? 'User blocked' : 'User unblocked'
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
