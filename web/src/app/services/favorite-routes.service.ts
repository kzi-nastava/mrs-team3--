import { Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type FavoriteRoute = {
  id: string;

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

  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
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
    this.http.get<FavoriteRoute[]>(this.API)
      .subscribe({
        next: routes => {
          this.favorites.set(routes);
        },
        error: err => {
          console.error('Failed to load favorite routes', err);
        }
      });
  }

  add(route: Omit<FavoriteRoute, 'id'>): void {

  const exists = this.favorites().some(r =>
    this.sameRoute(r, route)
  );
  if (exists) return;

  this.http.post(this.API, route)
    .subscribe({
      error: err => console.error('Add favorite failed', err)
    });

  const newRoute: FavoriteRoute = {
    id: this.makeId(),
    ...route
  };

  this.favorites.set([...this.favorites(), newRoute]);
}


  remove(route: Omit<FavoriteRoute, 'id'>): void {

  this.http.request('delete', this.API, { body: route })
.subscribe({
    error: err => console.error('Remove favorite failed', err)
  });

  this.favorites.set(
    this.favorites().filter(r => !this.sameRoute(r, route))
  );
}


  toggle(route: Omit<FavoriteRoute, 'id'>): void {
    const exists = this.favorites().some(r =>
      this.sameRoute(r, route)
    );

    if (exists) this.remove(route);
    else this.add(route);
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