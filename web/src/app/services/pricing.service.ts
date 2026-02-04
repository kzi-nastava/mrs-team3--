import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface PricingResponse {
  id: number;
  standardBasePrice: number;
  luxuryBasePrice: number;
  vanBasePrice: number;
  pricePerKm: number;
}

export interface PricingChangeRequest {
  standardBasePrice?: number;
  luxuryBasePrice?: number;
  vanBasePrice?: number;
  pricePerKm?: number;
}

export interface PricingConstraints {
  minBasePrice: number;
  maxBasePrice: number;
  minPricePerKm: number;
  maxPricePerKm: number;
  businessRules: {
    rule1: string;
    rule2: string;
  };
}

export interface PricingChange {
  field: string;
  oldValue: number;
  newValue: number;
  difference: number;
}

@Injectable({
  providedIn: 'root'
})
export class PricingService {

  private apiUrl = env.API_URL + "/api/pricing";

  constructor(private http: HttpClient) {}


  getCurrentPricing(): Observable<PricingResponse> {
    return this.http.get<PricingResponse>(this.apiUrl);
  }

  updatePricing(request: PricingChangeRequest): Observable<PricingResponse> {
    return this.http.put<PricingResponse>(this.apiUrl, request);
  }

  getConstraints(): Observable<PricingConstraints> {
    return this.http.get<PricingConstraints>(`${this.apiUrl}/constraints`);
  }

  calculateChanges(
    currentPricing: PricingResponse,
    newRequest: PricingChangeRequest
  ): PricingChange[] {
    const changes: PricingChange[] = [];

    if (newRequest.standardBasePrice !== undefined && 
        newRequest.standardBasePrice !== currentPricing.standardBasePrice) {
      changes.push({
        field: 'Standard Base Price',
        oldValue: currentPricing.standardBasePrice,
        newValue: newRequest.standardBasePrice,
        difference: newRequest.standardBasePrice - currentPricing.standardBasePrice
      });
    }

    if (newRequest.luxuryBasePrice !== undefined && 
        newRequest.luxuryBasePrice !== currentPricing.luxuryBasePrice) {
      changes.push({
        field: 'Luxury Base Price',
        oldValue: currentPricing.luxuryBasePrice,
        newValue: newRequest.luxuryBasePrice,
        difference: newRequest.luxuryBasePrice - currentPricing.luxuryBasePrice
      });
    }

    if (newRequest.vanBasePrice !== undefined && 
        newRequest.vanBasePrice !== currentPricing.vanBasePrice) {
      changes.push({
        field: 'Van Base Price',
        oldValue: currentPricing.vanBasePrice,
        newValue: newRequest.vanBasePrice,
        difference: newRequest.vanBasePrice - currentPricing.vanBasePrice
      });
    }

    if (newRequest.pricePerKm !== undefined && 
        newRequest.pricePerKm !== currentPricing.pricePerKm) {
      changes.push({
        field: 'Price Per Km',
        oldValue: currentPricing.pricePerKm,
        newValue: newRequest.pricePerKm,
        difference: newRequest.pricePerKm - currentPricing.pricePerKm
      });
    }

    return changes;
  }

  validatePricingRequest(
    request: PricingChangeRequest,
    constraints: PricingConstraints,
    currentPricing: PricingResponse
  ): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    const finalStandard = request.standardBasePrice ?? currentPricing.standardBasePrice;
    const finalLuxury = request.luxuryBasePrice ?? currentPricing.luxuryBasePrice;
    const finalVan = request.vanBasePrice ?? currentPricing.vanBasePrice;
    const finalPricePerKm = request.pricePerKm ?? currentPricing.pricePerKm;

    if (request.standardBasePrice !== undefined) {
      if (request.standardBasePrice < constraints.minBasePrice) {
        errors.push(`Standard base price must be at least ${constraints.minBasePrice}`);
      }
      if (request.standardBasePrice > constraints.maxBasePrice) {
        errors.push(`Standard base price cannot exceed ${constraints.maxBasePrice}`);
      }
    }

    if (request.luxuryBasePrice !== undefined) {
      if (request.luxuryBasePrice < constraints.minBasePrice) {
        errors.push(`Luxury base price must be at least ${constraints.minBasePrice}`);
      }
      if (request.luxuryBasePrice > constraints.maxBasePrice) {
        errors.push(`Luxury base price cannot exceed ${constraints.maxBasePrice}`);
      }
    }

    if (request.vanBasePrice !== undefined) {
      if (request.vanBasePrice < constraints.minBasePrice) {
        errors.push(`Van base price must be at least ${constraints.minBasePrice}`);
      }
      if (request.vanBasePrice > constraints.maxBasePrice) {
        errors.push(`Van base price cannot exceed ${constraints.maxBasePrice}`);
      }
    }

    if (request.pricePerKm !== undefined) {
      if (request.pricePerKm < constraints.minPricePerKm) {
        errors.push(`Price per km must be at least ${constraints.minPricePerKm}`);
      }
      if (request.pricePerKm > constraints.maxPricePerKm) {
        errors.push(`Price per km cannot exceed ${constraints.maxPricePerKm}`);
      }
    }

    if (finalLuxury <= finalStandard) {
      errors.push(
        `Luxury base price (${finalLuxury}) must be higher than standard base price (${finalStandard})`
      );
    }

    if (finalVan <= finalLuxury) {
      errors.push(
        `Van base price (${finalVan}) must be higher than luxury base price (${finalLuxury})`
      );
    }

    if (finalVan <= finalStandard) {
      errors.push(
        `Van base price (${finalVan}) must be higher than standard base price (${finalStandard})`
      );
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}