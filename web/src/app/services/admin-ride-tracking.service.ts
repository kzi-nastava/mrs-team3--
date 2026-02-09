import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface Location {
  lat: number;
  lng: number;
  address: string;
}

export interface StopStatus {
  stopIndex: number;
  location: Location;
  reached: boolean;
  reachedAt: string | null;
}

export interface PassengerInfo {
  id: number;
  name: string;
  surname: string;
  email: string;
}

export interface RideInviteInfo {
  id: number;
  email: string;
  trackingToken: string;
  createdAt: string;
}

export interface AdminRideTrackingData {
  rideId: number;
  status: 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'PANIC';
  
  // Locations
  startLocation: Location;
  endLocation: Location;
  stops: Location[];
  stopStatuses: StopStatus[];
  
  // Driver info
  driverCurrentLocation: Location | null;
  driverId: number;
  driverName: string;
  driverPhone: string | null;
  driverEmail: string;
  
  // Vehicle info
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  vehicleModel: string | null;
  vehicleRegistration: string | null;
  
  // Ride details
  distanceKm: number;
  estimatedTimeMinutes: number;
  remainingMinutes: number;
  calculatedPrice: number;
  
  // Timestamps
  createdAt: string;
  scheduledAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  
  // Special features
  babyTransport: boolean;
  petTransport: boolean;
  
  // Passengers and invites
  passengers: PassengerInfo[];
  creatorId: number;
  creatorName: string;
  invites: RideInviteInfo[];
  
  // Panic status
  panic: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminRideTrackingService {
  private apiUrl = env.API_URL + '/api/admin';

  constructor(private http: HttpClient) {}

  getRideByDriverId(driverId: number): Observable<AdminRideTrackingData> {
    return this.http.get<AdminRideTrackingData>(`${this.apiUrl}/drivers/${driverId}/current-ride`);
  }

  getRideById(rideId: number): Observable<AdminRideTrackingData> {
    return this.http.get<AdminRideTrackingData>(`${this.apiUrl}/rides/${rideId}`);
  }
}