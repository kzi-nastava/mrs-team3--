import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { 
  PricingService, 
  PricingResponse, 
  PricingChangeRequest, 
  PricingConstraints,
  PricingChange 
} from '../../services/pricing.service';
import { PricingConfirmationComponent } from '../pricing-confirmation/pricing-confirmation';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-pricing-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PricingConfirmationComponent],
  templateUrl: './pricing-management.html',
  styleUrls: ['./pricing-management.css']
})
export class PricingManagementComponent implements OnInit {
  
  Math = Math; 
  
  currentPricing: PricingResponse | null = null;
  constraints: PricingConstraints | null = null;
  
  formData = {
    standardBasePrice: 0,
    luxuryBasePrice: 0,
    vanBasePrice: 0,
    pricePerKm: 0
  };

  showConfirmationModal = false;
  pendingChanges: PricingChange[] = [];
  pendingRequest: PricingChangeRequest | null = null;

  isLoading = false;
  isSaving = false;

  validationErrors: string[] = [];

  constructor(
    private pricingService: PricingService,
    private messageService: MessageService,
    private location: Location,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPricingData();
  }

  private loadPricingData(): void {
    this.isLoading = true;
    
    forkJoin({
      pricing: this.pricingService.getCurrentPricing(),
      constraints: this.pricingService.getConstraints()
    }).subscribe({
      next: ({ pricing, constraints }) => {
        this.currentPricing = pricing;
        this.constraints = constraints;
        this.initializeForm(pricing);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error(error);
        this.isLoading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load pricing configuration'
        });
        this.cdr.detectChanges();
      }
    });
  }

  initializeForm(pricing: PricingResponse): void {
    this.formData = {
      standardBasePrice: pricing.standardBasePrice,
      luxuryBasePrice: pricing.luxuryBasePrice,
      vanBasePrice: pricing.vanBasePrice,
      pricePerKm: pricing.pricePerKm
    };
  }


  hasChanges(): boolean {
    if (!this.currentPricing) return false;
    
    return this.formData.standardBasePrice !== this.currentPricing.standardBasePrice ||
           this.formData.luxuryBasePrice !== this.currentPricing.luxuryBasePrice ||
           this.formData.vanBasePrice !== this.currentPricing.vanBasePrice ||
           this.formData.pricePerKm !== this.currentPricing.pricePerKm;
  }


  onSaveClick(): void {
    if (!this.currentPricing || !this.constraints) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Pricing data not loaded'
      });
      return;
    }

    const request: PricingChangeRequest = {};
    
    if (this.formData.standardBasePrice !== this.currentPricing.standardBasePrice) {
      request.standardBasePrice = this.formData.standardBasePrice;
    }
    if (this.formData.luxuryBasePrice !== this.currentPricing.luxuryBasePrice) {
      request.luxuryBasePrice = this.formData.luxuryBasePrice;
    }
    if (this.formData.vanBasePrice !== this.currentPricing.vanBasePrice) {
      request.vanBasePrice = this.formData.vanBasePrice;
    }
    if (this.formData.pricePerKm !== this.currentPricing.pricePerKm) {
      request.pricePerKm = this.formData.pricePerKm;
    }

    const validation = this.pricingService.validatePricingRequest(
      request, 
      this.constraints, 
      this.currentPricing
    );
    
    if (!validation.valid) {
      this.validationErrors = validation.errors;
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Failed',
        detail: validation.errors.join('; ')
      });
      return;
    }

    this.validationErrors = [];

    this.pendingChanges = this.pricingService.calculateChanges(this.currentPricing, request);
    this.pendingRequest = request;

    this.showConfirmationModal = true;
  }

  onConfirmChanges(): void {
    if (!this.pendingRequest) return;

    this.isSaving = true;
    this.showConfirmationModal = false;

    this.pricingService.updatePricing(this.pendingRequest).subscribe({
      next: (updatedPricing) => {
        this.currentPricing = updatedPricing;
        this.initializeForm(updatedPricing);
        this.isSaving = false;
        
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Pricing updated successfully',
          life: 5000
        });

        this.pendingRequest = null;
        this.pendingChanges = [];
      },
      error: (error) => {
        this.isSaving = false;
        
        const errorMessage = error.error?.message || 'Failed to update pricing';
        
        this.messageService.add({
          severity: 'error',
          summary: 'Update Failed',
          detail: errorMessage,
          life: 7000
        });

        this.pendingRequest = null;
        this.pendingChanges = [];
      }
    });
  }

  onCancelChanges(): void {
    this.showConfirmationModal = false;
    this.pendingRequest = null;
    this.pendingChanges = [];
  }

 
  resetForm(): void {
    if (this.currentPricing) {
      this.initializeForm(this.currentPricing);
      this.validationErrors = [];
      this.messageService.add({
        severity: 'info',
        summary: 'Form Reset',
        detail: 'Changes discarded'
      });
    }
  }

  formatCurrency(value: number): string {
    return value.toFixed(2) + ' RSD';
  }

  goBack(): void {
    this.location.back();
  }
}