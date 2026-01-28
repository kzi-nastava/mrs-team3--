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

export interface DriverRide {
  rideId: number;
  status: 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  startLocation: Location;
  endLocation: Location;
  stops: Location[];
  stopStatuses: StopStatus[];
  estimatedTimeMinutes: number;
  remainingMinutes: number;
  scheduledAt: string | null;
  startedAt: string | null;
  distance: number;
  calculatedPrice: number;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  passengerCount: number;
  creatorName: string;
}

export interface PendingRide {
  rideId: number;
  startLocation: Location;
  endLocation: Location;
  stops: Location[];
  estimatedTimeMinutes: number;
  createdAt: string;
  distance: number;
  calculatedPrice: number;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  passengerCount: number;
}

export interface FinishRideResponse {
  rideId: number;
  status: string;
  startedAt: string;
  finishedAt: string;
  calculatedPrice: number;
  hasNextRide: boolean;
  nextRideId: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class DriverRideService {
  private apiUrl = env.API_URL + '/api/drivers';

  constructor(private http: HttpClient) {}

  // Get driver's assigned rides
  getMyRides(): Observable<DriverRide[]> {
    return this.http.get<DriverRide[]>(`${this.apiUrl}/all-rides`);
  }

  // Get pending rides driver can accept
  getPendingRides(): Observable<PendingRide[]> {
    return this.http.get<PendingRide[]>(`${this.apiUrl}/pending-rides`);
  }

  // Accept a pending ride
  acceptRide(rideId: number): Observable<DriverRide> {
    return this.http.post<DriverRide>(
      `${this.apiUrl}/rides/${rideId}/accept`,
      {}
    );
  }

  // Start a ride
  startRide(rideId: number): Observable<DriverRide> {
    return this.http.post<DriverRide>(
      `${this.apiUrl}/rides/${rideId}/start`,
      {}
    );
  }

  finishRide(rideId: number, actualEndLocation: Location | null): Observable<FinishRideResponse> {
    return this.http.post<FinishRideResponse>(
      `${this.apiUrl}/rides/${rideId}/finish`,
      { actualEndLocation }
    );
  }

  // Update driver location
  updateLocation(latitude: number, longitude: number): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/location`,
      { latitude, longitude }
    );
  }

  // Move to start (teleport)
  moveToStart(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/move-to-start`, {});
  }

  // Mark stop as reached
  reachStop(stopIndex: number): Observable<DriverRide> {
    return this.http.post<DriverRide>(
      `${this.apiUrl}/reach-stop`,
      { stopIndex }
    );
  }

  cancelRide(rideId: number, reason: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/${rideId}/cancel`,
      { reason }
    );
  }

  panicRide(rideId: number): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/rides/${rideId}/panic`,
      {}
    );
  }
}
