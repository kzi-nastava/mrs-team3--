import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { Util } from '../util/util';


export type VehicleType = 'STANDARD' | 'VAN' | 'LUXURY';

export interface Location {
  lat: number;
  lng: number;
  name: string;
}

export interface RideBookingData {
  pickup: Location | null;
  stops: Location[];
  destination: Location | null;

  vehicleType: VehicleType;
  babyTransport: boolean;
  petTransport: boolean;
  passengers: number;
}

@Injectable({
  providedIn: 'root'
})
export class RideBookingService {
  private rideBookingDataSubject = new BehaviorSubject<RideBookingData>({
    pickup: null,
    stops: [],
    destination: null,

    vehicleType: 'STANDARD',
    babyTransport: false,
    petTransport: false,
    passengers: 1
  });

  public rideBookingData$ = this.rideBookingDataSubject.asObservable();

  private clearRouteSubject = new Subject<void>();
  public clearRoute$ = this.clearRouteSubject.asObservable();

  private calculateRouteSubject = new Subject<void>();
  public calculateRoute$ = this.calculateRouteSubject.asObservable();


  async setPickupLocation(location: Location): Promise<void> {
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideBookingDataSubject.value;

    this.rideBookingDataSubject.next({
      ...current,
      pickup: { ...location, name: address }
    });
  }

  async addStopLocation(location: Location): Promise<void> {
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideBookingDataSubject.value;

    this.rideBookingDataSubject.next({
      ...current,
      stops: [...current.stops, { ...location, name: address }]
    });
  }

  async updateStopLocation(index: number, location: Location): Promise<void> {
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideBookingDataSubject.value;

    const newStops = [...current.stops];
    newStops[index] = { ...location, name: address };

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

  async setDestinationLocation(location: Location): Promise<void> {
    const address = await Util.reverseGeocode(location.lat, location.lng);
    const current = this.rideBookingDataSubject.value;

    this.rideBookingDataSubject.next({
      ...current,
      destination: { ...location, name: address }
    });
  }

  setVehicleType(vehicleType: VehicleType): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({ ...current, vehicleType });
  }

  setBabyTransport(babyTransport: boolean): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({ ...current, babyTransport });
  }

  setPetTransport(petTransport: boolean): void {
    const current = this.rideBookingDataSubject.value;
    this.rideBookingDataSubject.next({ ...current, petTransport });
  }

  setPassengers(passengers: number): void {
    const current = this.rideBookingDataSubject.value;
    const safe = Math.max(1, Math.min(8, passengers || 1));
    this.rideBookingDataSubject.next({ ...current, passengers: safe });
  }

  getRideBookingData(): RideBookingData {
    return this.rideBookingDataSubject.value;
  }

  clearRoute(): void {
    const current = this.rideBookingDataSubject.value;

    this.rideBookingDataSubject.next({
      pickup: null,
      stops: [],
      destination: null,

      vehicleType: 'STANDARD',
      babyTransport: false,
      petTransport: false,
      passengers: 1
    });

    this.clearRouteSubject.next();
  }

  calculateRoute(): void {
    this.calculateRouteSubject.next();
  }
}
