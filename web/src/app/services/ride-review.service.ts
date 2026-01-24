import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface LocationDto {
  latitude: number;
  longitude: number;
  address: string;
}

export interface ExistingReview {
  driverRating: number;
  vehicleRating: number;
  comment: string | null;
  reviewedAt: string;
}

export interface RideReviewDetail {
  rideId: number;
  startLocation: LocationDto;
  endLocation: LocationDto;
  stops: LocationDto[];
  driverName: string;
  vehicleType: string;
  startTime: string;
  endTime: string;
  distance: number;
  duration: number;
  price: number;
  canReview: boolean;
  reviewDeadline: string;
  existingReview: ExistingReview | null;
}

export interface SubmitReviewRequest {
  driverRating: number;
  vehicleRating: number;
  comment: string | null;
}

export interface SubmitReviewResponse {
  rideId: number;
  driverRating: number;
  vehicleRating: number;
  comment: string | null;
  reviewedAt: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class RideReviewService {
  
  private apiUrl = `${env.API_URL}/api/rides`;

  constructor(private http: HttpClient) {}

  /**
   * Get ride details for review page
   */
  getRideForReview(rideId: number): Observable<RideReviewDetail> {
    return this.http.get<RideReviewDetail>(
      `${this.apiUrl}/${rideId}/review-details`
    );
  }

  /**
   * Submit or update a review
   */
  submitReview(rideId: number, request: SubmitReviewRequest): Observable<SubmitReviewResponse> {
    return this.http.post<SubmitReviewResponse>(
      `${this.apiUrl}/${rideId}/review`,
      request
    );
  }
}