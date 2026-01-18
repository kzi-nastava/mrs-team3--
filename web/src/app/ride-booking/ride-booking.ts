import { RideApiService } from '../services/ride-api.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormArray
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import {
  RideBookingService,
  Location,
  VehicleType
} from '../services/ride-booking.service';

import {
  FavoriteRoutesService,
  FavoriteRoute
} from '../services/favorite-routes.service';

interface Stop {
  id: number;
  location: string;
  suggestions: any[];
  showSuggestions: boolean;
}

@Component({
  selector: 'app-ride-booking',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './ride-booking.html',
  styleUrl: './ride-booking.css'
})
export class RideBookingComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  rideForm!: FormGroup;

  stops: Stop[] = [];

  pickupSuggestions: any[] = [];
  destinationSuggestions: any[] = [];
  showPickupSuggestions = false;
  showDestinationSuggestions = false;
  

  showConfirmModal = false;

  modalEstimatedTime = '';
  modalEstimatedPrice = '';


  showFavorites = false;
  favorites!: ReturnType<FavoriteRoutesService['getFavorites']>;

  private stopIdCounter = 0;
  private searchTimeout: any = null;



  constructor(
    private fb: FormBuilder,
    private rideBookingService: RideBookingService,
    private favoriteService: FavoriteRoutesService,
    private rideApiService: RideApiService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.favorites = this.favoriteService.getFavorites();

    this.rideBookingService.rideBookingData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.pickup) {
          this.rideForm.patchValue(
            { pickupLocation: data.pickup.name },
            { emitEvent: false }
          );
        }

        if (data.destination) {
          this.rideForm.patchValue(
            { destination: data.destination.name },
            { emitEvent: false }
          );
        }

        this.rideForm.patchValue({
          vehicleType: data.vehicleType,
          babyTransport: data.babyTransport,
          petTransport: data.petTransport,
          passengers: data.passengers
        }, { emitEvent: false });

        this.syncStops(data.stops);
      });

    this.rideForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(val => {
        this.rideBookingService.setVehicleType(val.vehicleType);
        this.rideBookingService.setBabyTransport(val.babyTransport);
        this.rideBookingService.setPetTransport(val.petTransport);
        this.rideBookingService.setPassengers(val.passengers);
      });

    this.rideForm.get('pickupLocation')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.onPickupInputChange());

    this.rideForm.get('destination')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.onDestinationInputChange());
  }



  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.rideForm = this.fb.group({
      pickupLocation: ['', Validators.required],
      destination: ['', Validators.required],

      vehicleType: ['STANDARD' as VehicleType, Validators.required],
      babyTransport: [false],
      petTransport: [false],
      passengers: [1, [Validators.required, Validators.min(1), Validators.max(8)]],

      // ✅ DODATO – NISTA POSTOJECE NIJE DIRANO
      passengerEmails: this.fb.array([])
    });
  }

  get passengerEmails(): FormArray {
    return this.rideForm.get('passengerEmails') as FormArray;
  }

  addPassengerEmail(input: HTMLInputElement): void {
    const email = input.value.trim();
    if (!email) return;

    this.passengerEmails.push(
      this.fb.control(email, Validators.email)
    );

    input.value = '';
  }

  removePassengerEmail(index: number): void {
    this.passengerEmails.removeAt(index);
  }

  toggleFavorites(): void {
    this.showFavorites = !this.showFavorites;
  }

  useFavoriteRoute(route: FavoriteRoute): void {
    this.rideForm.patchValue({
      pickupLocation: route.from,
      destination: route.to
    });

    this.stops = [];

    this.rideBookingService.setPickupLocation({
      lat: 0,
      lng: 0,
      name: route.from
    });

    this.rideBookingService.setDestinationLocation({
      lat: 0,
      lng: 0,
      name: route.to
    });

    this.showFavorites = false;
  }

  onPickupInputChange(): void {
    this.debounceSearch(this.rideForm.get('pickupLocation')?.value, 'pickup');
  }

  onDestinationInputChange(): void {
    this.debounceSearch(this.rideForm.get('destination')?.value, 'destination');
  }

  onStopInputChange(stopId: number, event: Event): void {
    const stop = this.stops.find(s => s.id === stopId);
    if (!stop) return;

    stop.location = (event.target as HTMLInputElement).value;
    this.debounceSearch(stop.location, 'stop', stopId);
  }

  private debounceSearch(value: string, type: 'pickup' | 'destination' | 'stop', stopId?: number): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);

    if (!value || value.length < 3) {
      if (type === 'pickup') this.showPickupSuggestions = false;
      if (type === 'destination') this.showDestinationSuggestions = false;
      if (type === 'stop') {
        const stop = this.stops.find(s => s.id === stopId);
        if (stop) stop.showSuggestions = false;
      }
      return;
    }

    this.searchTimeout = setTimeout(() => {
      this.searchLocation(value, type, stopId);
    }, 500);
  }

  selectPickupSuggestion(suggestion: any): void {
    this.selectLocation(suggestion, 'pickup');
  }

  selectDestinationSuggestion(suggestion: any): void {
    this.selectLocation(suggestion, 'destination');
  }

  selectStopSuggestion(stopId: number, suggestion: any): void {
    const location: Location = {
      lat: +suggestion.lat,
      lng: +suggestion.lon,
      name: suggestion.display_name
    };

    const index = this.stops.findIndex(s => s.id === stopId);
    if (index >= 0) {
      this.rideBookingService.updateStopLocation(index, location);
      this.stops[index].location = suggestion.display_name;
      this.stops[index].showSuggestions = false;
    }
  }

  private selectLocation(suggestion: any, type: 'pickup' | 'destination'): void {
    const location: Location = {
      lat: +suggestion.lat,
      lng: +suggestion.lon,
      name: suggestion.display_name
    };

    if (type === 'pickup') {
      this.rideForm.patchValue({ pickupLocation: location.name });
      this.showPickupSuggestions = false;
      this.rideBookingService.setPickupLocation(location);
    } else {
      this.rideForm.patchValue({ destination: location.name });
      this.showDestinationSuggestions = false;
      this.rideBookingService.setDestinationLocation(location);
    }
  }

  private searchLocation(query: string, type: 'pickup' | 'destination' | 'stop', stopId?: number): void {
    const url =
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query + ', Serbia')}&limit=5`;

    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (type === 'pickup') {
          this.pickupSuggestions = data;
          this.showPickupSuggestions = true;
        } else if (type === 'destination') {
          this.destinationSuggestions = data;
          this.showDestinationSuggestions = true;
        } else if (type === 'stop' && stopId !== undefined) {
          const stop = this.stops.find(s => s.id === stopId);
          if (stop) {
            stop.suggestions = data;
            stop.showSuggestions = true;
          }
        }
      });
  }

  addStop(): void {
    this.stops.push({
      id: this.stopIdCounter++,
      location: '',
      suggestions: [],
      showSuggestions: false
    });
  }

  removeStop(id: number): void {
    const index = this.stops.findIndex(s => s.id === id);
    if (index >= 0) {
      this.stops.splice(index, 1);
      this.rideBookingService.removeStopLocation(index);
    }
  }

  private syncStops(serviceStops: Location[]): void {
    if (serviceStops.length !== this.stops.length) {
      this.stops = serviceStops.map((s, i) => ({
        id: i,
        location: s.name,
        suggestions: [],
        showSuggestions: false
      }));
      this.stopIdCounter = this.stops.length;
    }
  }



 onBookRide(): void {
  if (this.rideForm.invalid) return;

  const data = this.rideBookingService.getRideBookingData();

  if (!data.pickup || !data.destination) {
    alert('Pickup and destination are required');
    return;
  }

  this.openConfirmModal(data);
}


openConfirmModal(data: any): void {

  const payload = {
    startLocation: {
      latitude: data.pickup.lat,
      longitude: data.pickup.lng,
      address: data.pickup.name
    },
    endLocation: {
      latitude: data.destination.lat,
      longitude: data.destination.lng,
      address: data.destination.name
    },
    stops: data.stops.map((s: any) => ({
      latitude: s.lat,
      longitude: s.lng,
      address: s.name
    })),
    vehicleType: data.vehicleType,
    babyTransport: data.babyTransport,
    petTransport: data.petTransport
  };

  this.rideApiService.estimateRoute(payload).subscribe({
    next: res => {
      this.modalEstimatedTime = `${res.estimatedTimeMinutes} min`;
      this.modalEstimatedPrice = `${res.estimatedPrice} din`;

      this.showConfirmModal = true;
    },
    error: err => {
      console.error(err);
      alert('Failed to calculate ride estimate');
    }
  });
}


closeConfirmModal(): void {
  this.showConfirmModal = false;
}


confirmCreateRide(): void {
  this.showConfirmModal = false;

  const data = this.rideBookingService.getRideBookingData();

  const payload = {
    startLocation: {
      latitude: data.pickup!.lat,
      longitude: data.pickup!.lng,
      address: data.pickup!.name
    },
    endLocation: {
      latitude: data.destination!.lat,
      longitude: data.destination!.lng,
      address: data.destination!.name
    },
    stops: data.stops.map(s => ({
      latitude: s.lat,
      longitude: s.lng,
      address: s.name
    })),
    passengerEmails: this.passengerEmails.value,
    vehicleType: data.vehicleType,
    babyTransport: data.babyTransport,
    petTransport: data.petTransport
  };

  this.rideApiService.createRide(payload).subscribe({
    next: () => {
      alert('Ride successfully created!');
      this.clearRoute();},
    error: err => {
      console.error(err);
      alert('Ride creation failed');
    }
  });
}





  clearRoute(): void {
  this.rideForm.reset({
    pickupLocation: '',
    destination: '',
    vehicleType: 'STANDARD',
    babyTransport: false,
    petTransport: false,
    passengers: 1
  });

  this.rideForm.setControl('passengerEmails', this.fb.array([]));

  this.stops = [];
  this.stopIdCounter = 0;

  this.rideBookingService.clearRoute();

}


  onSchedule(): void {
    alert('Schedule feature coming soon');
  }

  trackByStopId(index: number, stop: Stop): number {
    return stop.id;
  }
}
