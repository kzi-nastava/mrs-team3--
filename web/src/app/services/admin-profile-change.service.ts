import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface AdminProfileChangeRequest {
  requestId: number;
  driverId: number;
  driverEmail: string;
  driverName: string;
  driverSurname: string;
  requestedAt: string;
  status: string;
}

export interface AdminProfileChangeRequestDetails {
  requestId: number;
  status: string;
  requestedAt: string;

  driverId: number;
  driverEmail: string;
  driverFirstName: string;
  driverLastName: string;

  oldFirstName: string;
  oldLastName: string;
  oldPhoneNumber: string;
  oldAddress: string;

  newFirstName?: string;
  newLastName?: string;
  newPhoneNumber?: string;
  newAddress?: string;

  oldVehicleModel: string;
  oldVehicleRegistrationNumber: string;
  oldVehicleSeatingCapacity: number;
  oldVehicleType: string;
  oldBabyTransport: boolean;
  oldPetTransport: boolean;

  newVehicleModel?: string;
  newVehicleRegistrationNumber?: string;
  newVehicleSeatingCapacity?: number;
  newVehicleType?: string;
  newBabyTransport?: boolean;
  newPetTransport?: boolean;
}

export interface AdminProfileChangeDecision {
  approved: boolean;
  rejectReason?: string;
}



@Injectable({ providedIn: 'root' })
export class AdminProfileChangeService {

  private API = env.API_URL + '/api/admin/profile-change-requests';

  constructor(private http: HttpClient) {}

  getPending(): Observable<AdminProfileChangeRequest[]> {
    return this.http.get<AdminProfileChangeRequest[]>(
      `${this.API}/pending`
    );
  }

  getAll(): Observable<AdminProfileChangeRequest[]> {
    return this.http.get<AdminProfileChangeRequest[]>(this.API);
  }

  getDetails(
    requestId: number
  ): Observable<AdminProfileChangeRequestDetails> {
    return this.http.get<AdminProfileChangeRequestDetails>(
      `${this.API}/${requestId}`
    );
  }

  decideRequest(
  requestId: number,
  decision: AdminProfileChangeDecision
): Observable<void> {
  return this.http.post<void>(
    `${this.API}/${requestId}/decision`,
    decision
  );
}




}
