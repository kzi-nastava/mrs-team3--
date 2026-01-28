import {Injectable} from '@angular/core';
import {env} from '../../env/env';
import { HttpClient } from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Ride {
  id: number;
  startLocation: {
    lat: number;
    lng: number;
    address: string;
  };
  endLocation: {
    lat: number;
    lng: number;
    address: string;
  };
  stops: {
    lat: number;
    lng: number;
    address: string;
  }[];
  startTime: string;
}

@Injectable({
  providedIn: 'root'
})

export class IncomingRidesService {
  private apiUrl = env.API_URL + '/api/rides/incoming-rides';
  constructor(private http: HttpClient) {}

  getRides(): Observable<Ride[]> {
    return this.http.get<Ride[]>(`${this.apiUrl}/passenger`);
  }

  cancelIncomingRide(rideId: number) : Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/passenger/${rideId}/cancel`, {});
  }

}
