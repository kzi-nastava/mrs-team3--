import { Injectable, signal, WritableSignal } from '@angular/core';

export type FavoriteRoute = {
  id: string;  
  from: string;
  to: string;
  stops: string[];   
};

@Injectable({
  providedIn: 'root'
})
export class FavoriteRoutesService {

  private readonly favorites: WritableSignal<FavoriteRoute[]> = signal<FavoriteRoute[]>([]);

  getFavorites(): WritableSignal<FavoriteRoute[]> {
    return this.favorites;
  }

  add(route: Omit<FavoriteRoute, 'id'>): void {
    const exists = this.favorites().some(r =>
      this.sameRoute(r, route)
    );

    if (exists) return;

    const newRoute: FavoriteRoute = {
      id: this.makeId(),
      from: route.from,
      to: route.to,
      stops: route.stops ?? []
    };

    this.favorites.set([...this.favorites(), newRoute]);
  }

  remove(route: Pick<FavoriteRoute, 'from' | 'to' | 'stops'>): void {
    this.favorites.set(
      this.favorites().filter(r => !this.sameRoute(r, route))
    );
  }

  toggle(route: Omit<FavoriteRoute, 'id'>): void {
    const exists = this.favorites().some(r => this.sameRoute(r, route));
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
    const fromA = (a.from ?? '').trim().toLowerCase();
    const toA = (a.to ?? '').trim().toLowerCase();
    const fromB = (b.from ?? '').trim().toLowerCase();
    const toB = (b.to ?? '').trim().toLowerCase();

    if (fromA !== fromB) return false;
    if (toA !== toB) return false;

    const stopsA = (a.stops ?? []).map(s => (s ?? '').trim().toLowerCase());
    const stopsB = (b.stops ?? []).map(s => (s ?? '').trim().toLowerCase());

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
