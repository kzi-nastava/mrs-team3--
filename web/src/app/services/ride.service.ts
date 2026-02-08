import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import {Util} from '../util/util';

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

  private estimatedTimeSubject = new BehaviorSubject<string>('');
  public estimatedTime$ = this.estimatedTimeSubject.asObservable();

  private durationSecondsSubject = new BehaviorSubject<number | null>(null);
  public durationSeconds$ = this.durationSecondsSubject.asObservable();

  async setStartLocation(location: Location): Promise<void> {
    this.clearEstimate();
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideDataSubject.value;
    this.rideDataSubject.next({
      ...current,
      start: { ...location, name: address }
    });

    this.autoCalculateIfReady();
  }

  async setEndLocation(location: Location): Promise<void> {
    this.clearEstimate();
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideDataSubject.value;
    this.rideDataSubject.next({
      ...current,
      end: {...location, name: address}
    });

    this.autoCalculateIfReady();
  }

  getRideData(): RideData {
    return this.rideDataSubject.value;
  }

  clearRoute(): void {
    this.rideDataSubject.next({ start: null, end: null });
    this.estimatedTimeSubject.next('');
    this.durationSecondsSubject.next(null);
    this.clearRouteSubject.next();
  }

  calculateRoute(): void {
    this.calculateRouteSubject.next();
  }

  setDurationSeconds(seconds: number): void {
    console.log('setDurationSeconds called with:', seconds);
    this.durationSecondsSubject.next(seconds);
    this.estimatedTimeSubject.next(this.formatDuration(seconds));
  }

  private autoCalculateIfReady(): void {
    const { start, end } = this.rideDataSubject.value;
    if (start && end) this.calculateRoute();
  }

  private formatDuration(totalSeconds: number): string {
    const mins = Math.round(totalSeconds / 60);
    if (mins < 60) return `${mins} min`;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return `${h} h ${m} min`;
  }
  private clearEstimate(): void {
    this.estimatedTimeSubject.next('');
    this.durationSecondsSubject.next(null);
  }

}
