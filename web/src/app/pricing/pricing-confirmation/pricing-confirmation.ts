import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PricingChange } from '../../services/pricing.service';

@Component({
  selector: 'app-pricing-confirmation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pricing-confirmation.html',
  styleUrls: ['./pricing-confirmation.css']
})
export class PricingConfirmationComponent implements OnInit {
  @Input() changes: PricingChange[] = [];
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  positiveChanges: PricingChange[] = [];
  negativeChanges: PricingChange[] = [];

  ngOnInit(): void {
    this.categorizeChanges();
  }

  categorizeChanges(): void {
    this.positiveChanges = this.changes.filter(c => c.difference > 0);
    this.negativeChanges = this.changes.filter(c => c.difference < 0);
  }

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }

  formatCurrency(value: number): string {
    return value.toFixed(2) + ' RSD';
  }

  formatDifference(value: number): string {
    const sign = value > 0 ? '+' : '';
    return sign + value.toFixed(2) + ' RSD';
  }

  getChangeIcon(difference: number): string {
    return difference > 0 ? '📈' : '📉';
  }
}