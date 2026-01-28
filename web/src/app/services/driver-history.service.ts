import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { env } from '../../env/env';

export interface Review {
  driverRating: number | null;
  vehicleRating: number | null;
  comment: string | null;
}

export interface InconsistencyReport {
  id: number;
  message: string;
  reportedAt: string;
}

export interface DriverRide {
  id: number;
  startTime: string;
  endTime: string | null;
  startLocation: {
    address: string;
    latitude: number;
    longitude: number;
  };
  endLocation: {
    address: string;
    latitude: number;
    longitude: number;
  };
  stops: {
    address: string;
    latitude: number;
    longitude: number;
  }[];
  registeredPassengers: string[];
  invitedPassengers: string[];
  price: number;
  distance: number;
  duration: number;
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  status: 'COMPLETED' | 'CANCELLED_BY_DRIVER' | 'CANCELLED_BY_PASSENGER' | 'FINISHED_EARLY';
  cancelledBy?: string;
  cancelReason?: string;
  hadPanic?: boolean;
  reviews?: Review[];
  inconsistencyReports?: InconsistencyReport[];
}

@Injectable({
  providedIn: 'root'
})
export class DriverHistoryService {
  private apiUrl = env.API_URL + '/api/drivers';

  constructor(private http: HttpClient) {}

  getRides(driverId: number, startDate?: string, endDate?: string): Observable<DriverRide[]> {
    let params = new HttpParams();
    
    if (startDate) {
      // Convert YYYY-MM-DD to ISO DateTime (start of day)
      const startDateTime = new Date(startDate);
      startDateTime.setHours(0, 0, 0, 0);
      params = params.set('startDate', startDateTime.toISOString());
    }
    
    if (endDate) {
      // Convert YYYY-MM-DD to ISO DateTime (end of day)
      const endDateTime = new Date(endDate);
      endDateTime.setHours(23, 59, 59, 999);
      params = params.set('endDate', endDateTime.toISOString());
    }

    return this.http.get<any[]>(`${this.apiUrl}/${driverId}/rides`, { params })
      .pipe(
        map(rides => rides.map(r => this.mapToDriverRide(r)))
      );
  }

  getRideDetail(driverId: number, rideId: number): Observable<DriverRide> {
  return this.http.get<any>(`${this.apiUrl}/${driverId}/rides/${rideId}`)
    .pipe(
      map(r => {
        // console.log('API Response for ride detail:', r);
        // console.log('Planned stops:', r.plannedStops);
        return this.mapToDriverRideDetail(r);
      })
    );
}

  getRideById(driverId: number, rideId: number): Observable<DriverRide> {
    return this.getRideDetail(driverId, rideId);
  }

  private mapToDriverRide(apiRide: any): DriverRide {
  const duration = this.calculateDuration(apiRide.startedAt, apiRide.finishedAt);

  return {
    id: apiRide.rideId,
    startTime: apiRide.startedAt,
    endTime: apiRide.finishedAt,
    startLocation: {
      address: apiRide.startAddress,
      latitude: apiRide.startLatitude || 45.2671,
      longitude: apiRide.startLongitude || 19.8335
    },
    endLocation: {
      address: apiRide.endAddress,
      latitude: apiRide.endLatitude || 45.2671,
      longitude: apiRide.endLongitude || 19.8335
    },
    stops: (apiRide.plannedStops || []).map((stop: any) => ({
      address: stop.address,
      latitude: stop.latitude,
      longitude: stop.longitude
    })),
    registeredPassengers: apiRide.passengerNames || [],
    invitedPassengers: apiRide.invitedPassengers || [],
    price: apiRide.price,
    distance: apiRide.distance,
    duration: duration,
    vehicleType: apiRide.vehicleType || 'STANDARD',
    status: this.mapStatus(apiRide.status, apiRide.wasCancelled, apiRide.cancelledBy, apiRide.hadPanicEvent),
    cancelledBy: this.mapCancelledBy(apiRide.cancelledBy),
    cancelReason: apiRide.terminationReason,
    hadPanic: apiRide.hadPanicEvent || false,
    reviews: apiRide.reviews || [],
    inconsistencyReports: (apiRide.inconsistencyReports || []).map((report: any) => ({
      id: report.reportId,
      message: report.message,
      reportedAt: report.reportedAt
    }))
  };
}

private mapToDriverRideDetail(apiRide: any): DriverRide {
  const duration = this.calculateDuration(apiRide.startedAt, apiRide.finishedAt);

  return {
    id: apiRide.rideId,
    startTime: apiRide.startedAt,
    endTime: apiRide.finishedAt,
    startLocation: {
      address: apiRide.startAddress,
      latitude: apiRide.startLatitude || 45.2671,
      longitude: apiRide.startLongitude || 19.8335
    },
    endLocation: {
      address: apiRide.endAddress,
      latitude: apiRide.endLatitude || 45.2671,
      longitude: apiRide.endLongitude || 19.8335
    },
    stops: (apiRide.plannedStops || []).map((stop: any) => ({
      address: stop.address,
      latitude: stop.latitude,
      longitude: stop.longitude
    })),
    registeredPassengers: apiRide.passengerNames || [],
    invitedPassengers: apiRide.invitedPassengers || [],
    price: apiRide.price,
    distance: apiRide.distance,
    duration: duration,
    vehicleType: apiRide.vehicleType || 'STANDARD',
    status: this.mapStatus(
      apiRide.status, 
      apiRide.wasCancelled, 
      apiRide.cancelledBy, 
      apiRide.hadPanicEvent,
      apiRide.wasFinishedEarly
    ),
    cancelledBy: this.mapCancelledBy(apiRide.cancelledBy),
    cancelReason: apiRide.terminationReason,
    hadPanic: apiRide.hadPanicEvent || false,
    reviews: apiRide.reviews || [],
    inconsistencyReports: (apiRide.inconsistencyReports || []).map((report: any) => ({
      id: report.reportId,
      message: report.message,
      reportedAt: report.reportedAt
    }))
  };
}


  private mapStatus(
    status: string, 
    wasCancelled: boolean, 
    cancelledBy: string,
    hadPanic: boolean,
    wasFinishedEarly?: boolean
  ): 'COMPLETED' | 'CANCELLED_BY_DRIVER' | 'CANCELLED_BY_PASSENGER'  | 'FINISHED_EARLY' {
    // Handle panic events first
    
    
    // Handle cancellations
    if (wasCancelled || status === 'CANCELLED') {
      if (cancelledBy === 'DRIVER') {
        return 'CANCELLED_BY_DRIVER';
      } else if (cancelledBy === 'PASSENGER') {
        return 'CANCELLED_BY_PASSENGER';
      }
      // If cancelled but no specific party, default to driver
      return 'CANCELLED_BY_DRIVER';
    }
    
    // Handle finished early
    if (wasFinishedEarly || status === 'FINISHED_EARLY') {
      return 'FINISHED_EARLY';
    }
    
    // Handle completed
    if (status === 'COMPLETED') {
      return 'COMPLETED';
    }
    
    // For any other status (PENDING, IN_PROGRESS, etc.), default to COMPLETED
    // This shouldn't happen in history, but just in case
    return 'COMPLETED';
  }

  private mapCancelledBy(cancelledBy: string | null): string | undefined {
    if (!cancelledBy) return undefined;
    
    if (cancelledBy === 'DRIVER') return 'Driver';
    if (cancelledBy === 'PASSENGER') return 'Passenger';
    
    return cancelledBy;
  }

  private calculateDuration(startTime: string | null, endTime: string | null): number {
    if (!startTime || !endTime) return 0;
    
    const start = new Date(startTime).getTime();
    const end = new Date(endTime).getTime();
    
    return Math.round((end - start) / 60000);
  }
}