import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

export interface DriverStatusResponse {
  active: boolean;
  activityRequest: boolean;
  inRide: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DriverService {

  private apiUrl = env.API_URL + "/api/drivers";

  constructor(private http: HttpClient) {}

  registerDriver(payload: any): Observable<any> {
    return this.http.post(this.apiUrl, payload);
  }

  setActiveStatus(): Observable<DriverStatusResponse> {
    return this.http.put<DriverStatusResponse>(`${this.apiUrl}/change-active-status`, {});
  }

  getStatus(): Observable<DriverStatusResponse> {
    return this.http.get<DriverStatusResponse>(`${this.apiUrl}/status`);
  }

}
