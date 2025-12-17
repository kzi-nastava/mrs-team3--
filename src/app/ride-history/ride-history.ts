import { Component, signal, Signal, computed } from '@angular/core';

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
};    

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [],
  templateUrl: './ride-history.html',
  styleUrl: './ride-history.css',
})
export class RideHistoryComponent {

  protected rides: Signal<Ride[]> = signal([
    {
      id: 1,
      start: '10:00',
      end: '10:25',
      from: 'Main Street',
      to: 'University',
      passengers: ["prle", "Andjela"],
      price: 85,
      cancelled: 'No',
      panic: 'No'
    },
    {
      id: 2,
      start: '12:30',
      end: '12:55',
      from: 'Airport',
      to: 'City Center',
      passengers: ["Marko", "Jovan", "Ana"],
      price: 150,
      cancelled: 'Yes (Driver)',
      panic: 'No'
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
        result = String(av).localeCompare(String(bv), undefined, { numeric: true, sensitivity: 'base' });
      }
      return asc ? result : -result;
    });
    return arr;
  });

  protected setSort(key: keyof Ride): void {
    if (this.sortKey() === key) {
      this.sortAsc.set(!this.sortAsc());
    } else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected viewReport(): void {
    // Placeholder – will be implemented in the future
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
