import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DriverService {

  private apiUrl = 'http://localhost:8080/api/drivers';

  constructor(private http: HttpClient) {}

  registerDriver(payload: any): Observable<any> {
    return this.http.post(this.apiUrl, payload);
  }
}
