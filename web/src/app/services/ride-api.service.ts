import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface LocationRequest {
  latitude: number;
  longitude: number;
  address: string;
}

export interface CreateRideRequest {
  startLocation: LocationRequest;
  endLocation: LocationRequest;
  stops: LocationRequest[];
  passengerEmails: string[];
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  babyTransport: boolean;
  petTransport: boolean;
  scheduledAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RideApiService {

  private apiUrl = env.API_URL + "/api/rides";

  constructor(private http: HttpClient) {}

  createRide(payload: CreateRideRequest): Observable<any> {
    return this.http.post(this.apiUrl, payload);
  }

  estimateRoute(payload: any): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/estimate-route`,
      payload
    );
  }
}
