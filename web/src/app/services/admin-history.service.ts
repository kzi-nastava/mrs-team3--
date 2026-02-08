import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

type RideStatus = string;

export type Location = {
  lat: number;
  lng: number;
  address: string;
};

export type AdminRideSummary = {
  id: number;
  status: RideStatus;

  startLocation: Location;
  endLocation: Location;

  startTime: string;
  endTime: string | null;

  favorite: boolean;
  price: number;
  panic: boolean;
};

export type InconsistencyReportItemResponse = {
  id?: number;
  reportText?: string;
  createdAt?: string;
};

export type AdminRideDetails = AdminRideSummary & {
  stops: Location[];

  driverName: string;
  passengerEmails: string[];

  driverReview: number | null;
  rideReview: number | null;

  inconsistencyReports: InconsistencyReportItemResponse[];

  cancellationReason?: string | null;
};


export type AdminSortBy =
  | 'startTime'
  | 'endTime'
  | 'price'
  | 'status'
  | 'route'
  | 'panic';

export type SortDir = 'asc' | 'desc';

@Injectable({
  providedIn: 'root',
})
export class AdminHistoryService {
  private apiUrl = env.API_URL + '/api/rides/history/admin';

  constructor(private http: HttpClient) {}

  getAdminRides(
    from?: string,
    to?: string,
    sortBy: AdminSortBy = 'startTime',
    sortDir: SortDir = 'desc'
  ): Observable<AdminRideSummary[]> {
    let params = new HttpParams()
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.get<AdminRideSummary[]>(this.apiUrl, { params });
  }

  getAdminRideDetails(rideId: number): Observable<AdminRideDetails> {
    return this.http.get<AdminRideDetails>(`${this.apiUrl}/${rideId}`);
  }
}
