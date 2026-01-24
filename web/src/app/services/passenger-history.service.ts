import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { env } from '../../env/env';
import {PassengerRideDetails, PassengerRideSummary} from '../ride-history/passenger/passenger-history';

export interface Ride {
  id: number;
  startTime: string;
  endTime: string;
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
  passengers: string[];
  price: number;
  distance: number;
  duration: number;
  vehicleType: 'STANDARD' | 'VAN' | 'LUXURY';
  babyTransport: boolean;
  petTransport: boolean;
  driverName: string;
  status: 'COMPLETED' | 'CANCELLED_BY_DRIVER' | 'CANCELLED_BY_PASSENGER' | 'PANIC';
  cancelledBy?: string;
  cancelReason?: string;
  rating?: number;
  favorite: boolean;
  inconsistencyReport?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class RideHistoryService {
  private apiUrl = env.API_URL + '/api/rides/history';

  // Mock data for now
  private mockRides: Ride[] = [
    {
      id: 1,
      startTime: '2026-01-15T10:00:00',
      endTime: '2026-01-15T10:25:00',
      startLocation: {
        address: 'Bulevar oslobođenja 46, Novi Sad',
        latitude: 45.2671,
        longitude: 19.8335
      },
      endLocation: {
        address: 'Trg Dositeja Obradovića 6, Novi Sad',
        latitude: 45.2537,
        longitude: 19.8425
      },
      stops: [],
      passengers: ['petar.petrovic@example.com', 'ana.jovic@example.com'],
      price: 350,
      distance: 3.2,
      duration: 25,
      vehicleType: 'STANDARD',
      babyTransport: false,
      petTransport: true,
      driverName: 'Marko Marković',
      status: 'COMPLETED',
      rating: 5,
      favorite: false
    },
    {
      id: 2,
      startTime: '2026-01-14T12:30:00',
      endTime: '2026-01-14T13:15:00',
      startLocation: {
        address: 'Novosadski sajam, Novi Sad',
        latitude: 45.2396,
        longitude: 19.8227
      },
      endLocation: {
        address: 'Petrovaradinska tvrđava, Novi Sad',
        latitude: 45.2541,
        longitude: 19.8656
      },
      stops: [
        {
          address: 'Štrand, Novi Sad',
          latitude: 45.2467,
          longitude: 19.8518
        }
      ],
      passengers: ['milan.nikolic@example.com', 'jovana.ilic@example.com', 'stefan.jovic@example.com'],
      price: 520,
      distance: 5.8,
      duration: 45,
      vehicleType: 'VAN',
      babyTransport: false,
      petTransport: true,
      driverName: 'Nikola Nikolić',
      status: 'CANCELLED_BY_DRIVER',
      cancelledBy: 'Driver',
      cancelReason: 'Vehicle malfunction',
      favorite: false
    },
    {
      id: 3,
      startTime: '2026-01-13T08:15:00',
      endTime: '2026-01-13T08:40:00',
      startLocation: {
        address: 'Futoška 46, Novi Sad',
        latitude: 45.2622,
        longitude: 19.8156
      },
      endLocation: {
        address: 'BIG Shopping Center, Novi Sad',
        latitude: 45.2445,
        longitude: 19.8092
      },
      stops: [],
      passengers: ['dragana.petrovic@example.com'],
      price: 410,
      distance: 4.1,
      duration: 25,
      babyTransport: false,
      petTransport: true,
      vehicleType: 'LUXURY',
      driverName: 'Igor Jovanović',
      status: 'COMPLETED',
      rating: 4,
      favorite: true,
      inconsistencyReport: ['Driver took a longer route than necessary']
    },
    {
      id: 4,
      startTime: '2026-01-12T18:45:00',
      endTime: '2026-01-12T18:50:00',
      startLocation: {
        address: 'Narodna bašta, Novi Sad',
        latitude: 45.2552,
        longitude: 19.8451
      },
      endLocation: {
        address: 'Dunavska 27, Novi Sad',
        latitude: 45.2543,
        longitude: 19.8468
      },
      stops: [],
      passengers: ['milan.savic@example.com'],
      price: 180,
      distance: 0.8,
      duration: 5,
      babyTransport: false,
      petTransport: true,
      vehicleType: 'STANDARD',
      driverName: 'Aleksandar Ilić',
      status: 'PANIC',
      rating: 1,
      favorite: false
    },
    {
      id: 5,
      startTime: '2026-01-11T16:20:00',
      endTime: '2026-01-11T17:05:00',
      startLocation: {
        address: 'Železnička stanica, Novi Sad',
        latitude: 45.2511,
        longitude: 19.8361
      },
      endLocation: {
        address: 'Veternik, Novi Sad',
        latitude: 45.2897,
        longitude: 19.8189
      },
      stops: [
        {
          address: 'Limanski park, Novi Sad',
          latitude: 45.2723,
          longitude: 19.8312
        }
      ],
      passengers: ['jelena.djordjevic@example.com', 'milos.stojanovic@example.com'],
      price: 680,
      distance: 7.4,
      duration: 45,
      babyTransport: false,
      petTransport: true,
      vehicleType: 'STANDARD',
      driverName: 'Darko Petrović',
      status: 'COMPLETED',
      rating: 5,
      favorite: false
    },
    {
      id: 6,
      startTime: '2026-01-10T14:00:00',
      endTime: '',
      startLocation: {
        address: 'Spens, Novi Sad',
        latitude: 45.2501,
        longitude: 19.8318
      },
      endLocation: {
        address: 'Futog centar, Novi Sad',
        latitude: 45.2384,
        longitude: 19.7125
      },
      stops: [],
      passengers: ['ana.pavlovic@example.com'],
      price: 0,
      distance: 0,
      duration: 0,
      babyTransport: false,
      petTransport: true,
      vehicleType: 'STANDARD',
      driverName: 'Predrag Jović',
      status: 'CANCELLED_BY_PASSENGER',
      cancelledBy: 'Passenger',
      cancelReason: 'Changed plans',
      favorite: false
    },
    {
  id: 7,
  startTime: '2026-01-16T09:10:00',
  endTime: '2026-01-16T09:55:00',
  startLocation: {
    address: 'Detelinarska pijaca, Novi Sad',
    latitude: 45.2679,
    longitude: 19.8056
  },
  endLocation: {
    address: 'Klinički centar Vojvodine, Novi Sad',
    latitude: 45.2586,
    longitude: 19.8321
  },
  stops: [
    {
      address: 'Limanski park, Novi Sad',
      latitude: 45.2723,
      longitude: 19.8312
    },
    {
      address: 'Promenada Shopping Mall, Novi Sad',
      latitude: 45.2619,
      longitude: 19.8338
    },
    {
      address: 'Spens, Novi Sad',
      latitude: 45.2501,
      longitude: 19.8318
    }
  ],
  passengers: [
    'ivan.mitrovic@example.com',
    'milica.stanic@example.com'
  ],
  price: 640,
  distance: 6.9,
  duration: 45,
  vehicleType: 'VAN',
  babyTransport: true,
  petTransport: false,
  driverName: 'Vladimir Stojanović',
  status: 'COMPLETED',
  rating: 5,
  favorite: false
}

  ];

  private ridesSubject = new BehaviorSubject<Ride[]>(this.mockRides);
  public rides$ = this.ridesSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Get all rides (mock for now)
  getRides(): Observable<PassengerRideSummary[]> {
    return this.http.get<PassengerRideSummary[]>(`${this.apiUrl}/passenger`);
  }

  getRideDetails(rideId: number): Observable<PassengerRideDetails> {
    return this.http.get<PassengerRideDetails>(`${this.apiUrl}/passenger/${rideId}`);
  }

  // Get rides with filters
  getRidesFiltered(startDate?: string, endDate?: string): Ride[] {
    let rides = this.ridesSubject.value;

    if (startDate) {
      rides = rides.filter(r => r.startTime >= startDate);
    }

    if (endDate) {
      rides = rides.filter(r => r.startTime <= endDate);
    }

    return rides;
  }

  // Toggle favorite status
  toggleFavorite(rideId: number): void {
    const rides = this.ridesSubject.value.map(r =>
      r.id === rideId ? { ...r, favorite: !r.favorite } : r
    );
    this.ridesSubject.next(rides);

    // TODO: Call backend API to persist favorite status
    // this.http.post(`${this.apiUrl}/${rideId}/favorite`, {}).subscribe();
  }

  // Get ride by ID
  getRideById(id: number): Ride | undefined {
    return this.ridesSubject.value.find(r => r.id === id);
  }

  // Update ride in local state
  updateRide(ride: Ride): void {
    const rides = this.ridesSubject.value.map(r =>
      r.id === ride.id ? ride : r
    );
    this.ridesSubject.next(rides);
  }
}
