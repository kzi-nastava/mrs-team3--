import { Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type VehicleType = 'STANDARD' | 'VAN' | 'LUXURY';

export type FavoriteRoute = {
  id: string;
  rideId: number;

  from: {
    address: string;
    latitude: number;
    longitude: number;
  };

  to: {
    address: string;
    latitude: number;
    longitude: number;
  };

  stops: {
    address: string;
    latitude: number;
    longitude: number;
  }[];

  vehicleType: VehicleType;
  babyTransport: boolean;
  petTransport: boolean;
};

type BackendLocation = {
  lat: number;
  lng: number;
  address: string;
};

type FavoriteRouteRequestDto = {
  rideId: number;
  from: { latitude: number; longitude: number; address: string };
  to: { latitude: number; longitude: number; address: string };
  stops: { latitude: number; longitude: number; address: string }[];
  vehicleType: VehicleType;
  babyTransport: boolean;
  petTransport: boolean;
};

type FavoriteRouteResponseDto = {
  id: number;
  rideId: number;
  from: BackendLocation;
  to: BackendLocation;
  stops: BackendLocation[];
  vehicleType: VehicleType;
  babyTransport: boolean;
  petTransport: boolean;
};


@Injectable({
  providedIn: 'root'
})
export class FavoriteRoutesService {

  private readonly favorites: WritableSignal<FavoriteRoute[]> =
    signal<FavoriteRoute[]>([]);


  private readonly API = 'http://localhost:8080/api/favorites';


  constructor(private http: HttpClient) {}

  getFavorites(): WritableSignal<FavoriteRoute[]> {
    return this.favorites;
  }


  loadFromBackend(): void {
    this.http.get<FavoriteRouteResponseDto[]>(this.API)
      .subscribe({
        next: rows => {
          const mapped: FavoriteRoute[] = (rows ?? []).map(r => ({
            id: String(r.id),
            rideId: r.rideId,
            from: {
              address: r.from.address,
              latitude: r.from.lat,
              longitude: r.from.lng,
            },
            to: {
              address: r.to.address,
              latitude: r.to.lat,
              longitude: r.to.lng,
            },
            stops: (r.stops ?? []).map(s => ({
              address: s.address,
              latitude: s.lat,
              longitude: s.lng,
            })),
            vehicleType: r.vehicleType,
            babyTransport: r.babyTransport,
            petTransport: r.petTransport,
          }));

          this.favorites.set(mapped);
        },
        error: err => console.error('Failed to load favorite routes', err),
      });
  }


  add(req: FavoriteRouteRequestDto): void {
    const exists = this.favorites().some(r => r.rideId === req.rideId);
    if (exists) return;

    this.http.post<void>(this.API, req).subscribe({
      next: () => this.loadFromBackend(),
      error: err => console.error('Add favorite failed', err),
    });
  }

  remove(req: FavoriteRouteRequestDto): void {
    this.http.request<void>('delete', this.API, { body: req }).subscribe({
      next: () => this.loadFromBackend(),
      error: err => console.error('Remove favorite failed', err),
    });
  }


  toggle(req: FavoriteRouteRequestDto): void {
    const exists = this.favorites().some(r => r.rideId === req.rideId);
    if (exists) this.remove(req);
    else this.add(req);
  }



  clear(): void {
    this.favorites.set([]);
  }

  private sameRoute(
  a: Pick<FavoriteRoute, 'from' | 'to' | 'stops'>,
  b: Pick<FavoriteRoute, 'from' | 'to' | 'stops'>
): boolean {

  const fromA = (a.from.address ?? '').trim().toLowerCase();
  const toA   = (a.to.address ?? '').trim().toLowerCase();

  const fromB = (b.from.address ?? '').trim().toLowerCase();
  const toB   = (b.to.address ?? '').trim().toLowerCase();

  if (fromA !== fromB) return false;
  if (toA !== toB) return false;

  const stopsA = (a.stops ?? []).map(s => s.address.trim().toLowerCase());
  const stopsB = (b.stops ?? []).map(s => s.address.trim().toLowerCase());

  if (stopsA.length !== stopsB.length) return false;

  for (let i = 0; i < stopsA.length; i++) {
    if (stopsA[i] !== stopsB[i]) return false;
  }

  return true;
}


  private makeId(): string {
    return `${Date.now()}_${Math.random().toString(16).slice(2)}`;
  }
}
