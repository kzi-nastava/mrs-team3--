import { Component, signal, computed, OnInit } from '@angular/core';
import { FavoriteRoutesService } from '../services/favorite-routes.service';

type Ride = {
  id: number;
  start: string;
  end: string;
  from: string;
  to: string;
  passengers: string[];
  price: number;
  cancelled: string;
  panic: string;
  favorite: boolean;
};

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [],
  templateUrl: './ride-history.html',
  styleUrl: './ride-history.css',
})
export class RideHistoryComponent implements OnInit {

  constructor(private favoriteService: FavoriteRoutesService) {}

  protected rides = signal<Ride[]>([
    {
      id: 1,
      start: '10:00',
      end: '10:25',
      from: 'Main Street',
      to: 'University',
      passengers: ['prle', 'Andjela'],
      price: 85,
      cancelled: 'No',
      panic: 'No',
      favorite: false
    },
    {
      id: 2,
      start: '12:30',
      end: '12:55',
      from: 'Airport',
      to: 'City Center',
      passengers: ['Marko', 'Jovan', 'Ana'],
      price: 150,
      cancelled: 'Yes (Driver)',
      panic: 'No',
      favorite: false
    }
  ]);

  protected sortKey = signal<keyof Ride>('id');
  protected sortAsc = signal<boolean>(true);

  protected sortedRides = computed(() => {
    const key = this.sortKey();
    const asc = this.sortAsc();
    const arr = [...this.rides()];

    arr.sort((a, b) => {
      const av = this.getComparable(a, key);
      const bv = this.getComparable(b, key);

      let result: number;
      if (typeof av === 'number' && typeof bv === 'number') {
        result = av - bv;
      } else {
        result = String(av).localeCompare(String(bv), undefined, {
          numeric: true,
          sensitivity: 'base'
        });
      }

      return asc ? result : -result;
    });

    return arr;
  });

  ngOnInit(): void {
    const favorites = this.favoriteService.getFavorites();

    this.rides.set(
      this.rides().map(r => ({
        ...r,
        favorite: favorites().some(f =>
          f.from === r.from && f.to === r.to
        )
      }))
    );
  }

  protected setSort(key: keyof Ride): void {
    if (this.sortKey() === key) {
      this.sortAsc.set(!this.sortAsc());
    } else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected toggleFavorite(ride: Ride): void {
    const updated = this.rides().map(r =>
      r.id === ride.id ? { ...r, favorite: !r.favorite } : r
    );
    this.rides.set(updated);

    const route = {
      from: ride.from,
      to: ride.to,
      stops: [] as string[]
    };

    if (!ride.favorite) {
      this.favoriteService.add(route);
    } else {
      this.favoriteService.remove(route);
    }
  }

  protected viewReport(): void {
    // Placeholder – implement later
  }

  private getComparable(r: Ride, key: keyof Ride): number | string {
    switch (key) {
      case 'id':
        return r.id;
      case 'price':
        return r.price;
      case 'passengers':
        return r.passengers.length;
      default:
        return (r as any)[key] ?? '';
    }
  }
}
