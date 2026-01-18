import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

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
  type: 'STANDARD' | 'VAN' | 'LUXURY';
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

  private readonly API_URL = env.API_URL + "/api/profile";

  constructor(private http: HttpClient) {}


  getMyProfile(): Observable<
    DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
  > {
    return this.http.get<
      DriverProfileResponse | PassengerProfileResponse | AdminProfileResponse
    >(`${this.API_URL}/me`);
  }

 
  updateMyProfile(
    payload: UpdateUserProfileRequest
  ): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/me`, payload);
  }
}
