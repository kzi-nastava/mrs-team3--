import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import {env} from '../../env/env'

export interface ActiveVehicle {
  latitude: number;
  longitude: number;
  available: boolean;
  registrationNumber: string;
}

@Injectable({
  providedIn: 'root'
})
export class DriverLocationService {
  private readonly API_URL =env.API_URL + '/api/vehicles';
  
  private vehiclesSubject = new BehaviorSubject<ActiveVehicle[]>([]);
  public vehicles$ = this.vehiclesSubject.asObservable();

  constructor(private http: HttpClient) {}

  getActiveVehicles(): Observable<ActiveVehicle[]> {
    return this.http.get<ActiveVehicle[]>(`${this.API_URL}/active`).pipe(
      tap(vehicles => this.vehiclesSubject.next(vehicles))
    );
  }

  refreshVehicles(): void {
    this.getActiveVehicles().subscribe();
  }
}