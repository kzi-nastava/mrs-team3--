import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

export interface Location {
  lat: number;
  lng: number;
  name: string;
}

export interface RideBookingData {
  pickup: Location | null;
  stops: Location[];
  destination: Location | null;
}

@Injectable({
  providedIn: 'root'
})
export class RideBookingService {
  private rideBookingDataSubject = new BehaviorSubject<RideBookingData>({
    pickup: null,
    stops: [],
    destination: null
  });
  
  public rideBookingData$ = this.rideBookingDataSubject.asObservable();
  
  private clearRouteSubject = new Subject<void>();
  public clearRoute$ = this.clearRouteSubject.asObservable();
  
  private calculateRouteSubject = new Subject<void>();
  public calculateRoute$ = this.calculateRouteSubject.asObservable();

  setPickupLocation(location: Location): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({
      ...current,
      pickup: location
    });
  }

  addStopLocation(location: Location): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({
      ...current,
      stops: [...current.stops, location]
    });
  }

  updateStopLocation(index: number, location: Location): void {
    const current = this.rideBookingDataSubject.value;
    const newStops = [...current.stops];
    newStops[index] = location;
    this.rideBookingDataSubject.next({
      ...current,
      stops: newStops
    });
  }

  removeStopLocation(index: number): void {
    const current = this.rideBookingDataSubject.value;
    const newStops = current.stops.filter((_, i) => i !== index);
    this.rideBookingDataSubject.next({
      ...current,
      stops: newStops
    });
  }

  setDestinationLocation(location: Location): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({
      ...current,
      destination: location
    });
  }

  getRideBookingData(): RideBookingData {
    return this.rideBookingDataSubject.value;
  }

  clearRoute(): void {
    this.rideBookingDataSubject.next({
      pickup: null,
      stops: [],
      destination: null
    });
    this.clearRouteSubject.next();
  }

  calculateRoute(): void {
    this.calculateRouteSubject.next();
  }
}