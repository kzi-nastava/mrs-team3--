import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type UserRole = 'PASSENGER' | 'DRIVER' | 'ADMIN';

export interface BaseProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
}

export interface VehicleResponse {
  id: number;
  model: string;
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  registrationNumber: string;
  seatingCapacity: number;
  babyTransport: boolean;
  petTransport: boolean;
}

export interface DriverProfileResponse extends BaseProfile {
  vehicle: VehicleResponse;
  active: boolean;
}

export interface PassengerProfileResponse extends BaseProfile {}

export interface AdminProfileResponse extends BaseProfile {}

export interface UpdateUserProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {

  private readonly API_URL = 'http://localhost:8080/api/profile';

  constructor(private http: HttpClient) {}

  getProfile(userId: number): Observable<
    DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
  > {
    return this.http.get<
      DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
    >(`${this.API_URL}/${userId}`);
  }

  updateProfile(
    userId: number,
    payload: UpdateUserProfileRequest
  ): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${userId}`, payload);
  }
}
