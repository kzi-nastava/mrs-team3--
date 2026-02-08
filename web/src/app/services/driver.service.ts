import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

@Injectable({
  providedIn: 'root'
})
export class DriverService {

  private apiUrl = env.API_URL + "/api/drivers";

  constructor(private http: HttpClient) {}

  registerDriver(payload: any): Observable<any> {
    return this.http.post(this.apiUrl, payload);
  }

  setActiveStatus(): Observable<any> {
    return this.http.put(`${this.apiUrl}/change-active-status`, {});
  }
}
