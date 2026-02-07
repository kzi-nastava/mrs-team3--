import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { env } from '../../env/env';

/** Shared types (match your backend DTO naming!) */
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

  favorite: boolean; // inherited from PassengerRideSummaryResponse
  price: number;
  panic: boolean;

  // optional if/when you add it:
  cancelledBy?: string | null;
};

export type AdminRideDetails = AdminRideSummary & {
  stops: Location[];
  cancelReason?: string | null;

  // optional expansions for later:
  // driver?: { id: number; name: string; email: string };
  // passengers?: { id: number; name: string; email: string }[];
  // inconsistencyReports?: ...
  // reviews?: ...
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

  /**
   * Expanded details for a ride (only on click)
   *
   * Example:
   * GET /api/rides/history/admin/123
   */
  getAdminRideDetails(rideId: number): Observable<AdminRideDetails> {
    return this.http.get<AdminRideDetails>(`${this.apiUrl}/${rideId}`);
  }
}
