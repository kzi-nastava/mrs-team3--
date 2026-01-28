import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface Location {
  lat: number;
  lng: number;
  address: string;
}

export interface RideTrackingData {
  rideId: number;
  status: 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'PANIC';
  startLocation: Location;
  endLocation: Location;
  stops: Location[];
  driverCurrentLocation: Location | null;
  driverName: string;
  driverPhone: string | null;
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  vehicleModel: string | null;
  vehicleRegistration: string | null;
  distanceKm: number;
  estimatedTimeMinutes: number;
  remainingMinutes: number;
  startedAt: string | null;
  scheduledAt: string | null;
  stoppedEarly: boolean;
  createdAt: string;
}

export interface TokenValidation {
  valid: boolean;
  message: string;
}

export interface ReportResponse {
  reportId: number;
  rideId: number;
  message: string;
  reportedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class RideTrackingService {
  private apiUrl = env.API_URL + '/api/ride-tracking';

  constructor(private http: HttpClient) {}

  /**
   * Get current ride for authenticated passenger
   */
  getCurrentRide(): Observable<RideTrackingData> {
    return this.http.get<RideTrackingData>(`${this.apiUrl}/current`);
  }

  /**
   * Validate tracking token (for guests)
   */
  validateToken(token: string): Observable<TokenValidation> {
    return this.http.get<TokenValidation>(`${this.apiUrl}/validate/${token}`);
  }

  /**
   * Get ride by tracking token (for guests)
   */
  getRideByToken(token: string): Observable<RideTrackingData> {
    return this.http.get<RideTrackingData>(`${this.apiUrl}/token/${token}`);
  }

  /**
   * Report inconsistency for current ride (authenticated)
   */
  reportInconsistencyForCurrentRide(reportText: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/current/report`, {
      reportText
    });
  }

  /**
   * Report inconsistency by token (for guests)
   */
  reportInconsistencyByToken(token: string, reportText: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/token/${token}/report`, {
      reportText
    });
  }

  panic(rideId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${rideId}/panic`, {});
  }

  panicByToken(token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/token/${token}/panic`, {});
  }
}
