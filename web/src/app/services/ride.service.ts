import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

export interface Location {
  lat: number;
  lng: number;
  name: string;
}

export interface RideData {
  start: Location | null;
  end: Location | null;
}

@Injectable({
  providedIn: 'root'
})
export class RideService {
  // Save ride data using BehaviorSubject
  private rideDataSubject = new BehaviorSubject<RideData>({
    start: null,
    end: null
  });
  
  // Observable which components can subscribe to
  public rideData$ = this.rideDataSubject.asObservable();
  
  // Event for clearing the route
  private clearRouteSubject = new Subject<void>();
  public clearRoute$ = this.clearRouteSubject.asObservable();
  
  // Event for calculating the route
  private calculateRouteSubject = new Subject<void>();
  public calculateRoute$ = this.calculateRouteSubject.asObservable();

  setStartLocation(location: Location): void {
    const current = this.rideDataSubject.value;
    this.rideDataSubject.next({
      ...current,
      start: location
    });
  }

  setEndLocation(location: Location): void {
    const current = this.rideDataSubject.value;
    this.rideDataSubject.next({
      ...current,
      end: location
    });
  }


  getRideData(): RideData {
    return this.rideDataSubject.value;
  }

  clearRoute(): void {
    this.rideDataSubject.next({
      start: null,
      end: null
    });
    this.clearRouteSubject.next();
  }

  calculateRoute(): void {
    this.calculateRouteSubject.next();
  }
}