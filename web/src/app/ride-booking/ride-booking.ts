import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { RideApiService } from '../services/ride-api.service';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CreateRideRequest } from '../services/ride-api.service';
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
import { DriverLocationService } from '../services/driver-location.service';

interface Stop {
  id: number;
  location: string;
  suggestions: any[];
  showSuggestions: boolean;
}

@Component({
  selector: 'app-ride-booking',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, ToastModule],
  providers: [MessageService],
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

  isCalculating = false;
  private lastCalculationTime = 0;
  private CALCULATION_COOLDOWN = 2000;


  showFavorites = false;
  favorites!: ReturnType<FavoriteRoutesService['getFavorites']>;

  private stopIdCounter = 0;
  private searchTimeout: any = null;



  constructor(
    private fb: FormBuilder,
    private rideBookingService: RideBookingService,
    private favoriteService: FavoriteRoutesService,
    private rideApiService: RideApiService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef,
    private driverLocationService: DriverLocationService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.favorites = this.favoriteService.getFavorites();
    this.favoriteService.loadFromBackend();


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

  refreshDriverLocations(): void {
    this.driverLocationService.refreshVehicles();
  }

  private initForm(): void {
    this.rideForm = this.fb.group({
      pickupLocation: ['', Validators.required],
      destination: ['', Validators.required],

      vehicleType: ['STANDARD' as VehicleType, Validators.required],
      babyTransport: [false],
      petTransport: [false],
      passengers: [1, [Validators.required, Validators.min(1), Validators.max(8)]],

      passengerEmails: this.fb.array([]),
      scheduledAt: [null]
    });
  }

  clearScheduledAt(): void {
    this.rideForm.patchValue({ scheduledAt: null });
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
      pickupLocation: route.from.address,
      destination: route.to.address
    });

    this.stops = [];

    this.rideBookingService.setPickupLocationDirect({
      lat: route.from.latitude,
      lng: route.from.longitude,
      name: route.from.address
    });

    this.rideBookingService.setDestinationLocationDirect({
      lat: route.to.latitude,
      lng: route.to.longitude,
      name: route.to.address
    });

    this.rideBookingService.clearStops();

    route.stops.forEach(s => {
      this.rideBookingService.addStopLocationDirect({
        lat: s.latitude,
        lng: s.longitude,
        name: s.address
      });
    });

    this.syncStops(this.rideBookingService.getRideBookingData().stops);



    this.showFavorites = false;

    this.rideForm.patchValue({
      vehicleType: route.vehicleType,
      babyTransport: route.babyTransport,
      petTransport: route.petTransport
    }, { emitEvent: false });

    this.rideBookingService.setVehicleType(route.vehicleType);
    this.rideBookingService.setBabyTransport(route.babyTransport);
    this.rideBookingService.setPetTransport(route.petTransport);

    this.rideBookingService.calculateRoute();


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
      this.rideBookingService.updateStopLocationDirect(index, location);
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
      this.rideBookingService.setPickupLocationDirect(location);
    } else {
      this.rideForm.patchValue({ destination: location.name });
      this.showDestinationSuggestions = false;
      this.rideBookingService.setDestinationLocationDirect(location);
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

    if (this.isCalculating) {
      this.messageService.add({
        severity: 'info',
        summary: 'Please wait',
        detail: 'Calculation in progress...'
      });
      return;
    }

    const now = Date.now();
    const timeSinceLastCalc = now - this.lastCalculationTime;

    if (timeSinceLastCalc < this.CALCULATION_COOLDOWN) {
      const remainingTime = Math.ceil((this.CALCULATION_COOLDOWN - timeSinceLastCalc) / 1000);
      this.messageService.add({
        severity: 'info',
        summary: 'Too fast',
        detail: `Please wait ${remainingTime} second(s) before calculating again`
      });
      return;
    }

    const data = this.rideBookingService.getRideBookingData();

    if (!data.pickup || !data.destination) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Missing information',
        detail: 'Please select pickup and destination locations'
      });
      return;
    }

    this.lastCalculationTime = now;
    this.openConfirmModal(data);
  }


  openConfirmModal(data: any): void {
    this.isCalculating = true;

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

    const timeout = setTimeout(() => {
      this.isCalculating = false;
      this.cdr.detectChanges();
      this.messageService.add({
        severity: 'error',
        summary: 'Request timeout',
        detail: 'Route calculation is taking too long. Please try again.'
      });
    }, 15000);

    this.rideApiService.estimateRoute(payload).subscribe({
      next: res => {
        clearTimeout(timeout);
        this.isCalculating = false;
        this.modalEstimatedTime = `${res.estimatedTimeMinutes} min`;
        this.modalEstimatedPrice = `${res.estimatedPrice.toFixed(2)} din`;
        this.showConfirmModal = true;
        this.cdr.detectChanges();
      },
      error: err => {
        clearTimeout(timeout);
        this.isCalculating = false;
        this.cdr.detectChanges();

        let errorMessage = 'Unable to calculate route. Please try again.';

        if (err.status === 429) {
          errorMessage = 'Too many requests. Please wait a moment and try again.';
        } else if (err.status === 0) {
          errorMessage = 'Network error. Please check your connection.';
        }

        this.messageService.add({
          severity: 'error',
          summary: 'Calculation failed',
          detail: errorMessage
        });
      }
    });
  }


  closeConfirmModal(): void {
    this.showConfirmModal = false;
  }


  confirmCreateRide(): void {
    this.showConfirmModal = false;

    const data = this.rideBookingService.getRideBookingData();

    const payload: CreateRideRequest = {
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
      petTransport: data.petTransport,
      scheduledAt: this.toIsoIfPresent(this.rideForm.value.scheduledAt)
    };

    this.rideApiService.createRide(payload).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Ride created',
          detail: 'Your ride has been successfully created.'
        });
        this.clearRoute();
      },
      error: err => {
        console.error(err);
        const reason =
          err?.error?.reason ||
          err?.error?.message ||
          'UNKNOWN';
        this.showRideError(reason);
      }
    });
  }


  private toIsoIfPresent(value: string | null): string | undefined {
    if (!value) return undefined;
    return value;
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

    //this.stops = [];
    this.stopIdCounter = 0;

    this.rideBookingService.clearRoute();

  }


  onSchedule(): void {
    alert('Schedule feature coming soon');
  }

  trackByStopId(index: number, stop: Stop): number {
    return stop.id;
  }

  private showRideError(reason: string) {
    switch (reason) {
      case 'NO_ACTIVE_DRIVERS':
        this.messageService.add({
          severity: 'warn',
          summary: 'No active drivers',
          detail: 'There are currently no active drivers available.'
        });
        break;

      case 'NO_MATCHING_DRIVERS':
        this.messageService.add({
          severity: 'warn',
          summary: 'No matching drivers',
          detail: 'No driver matches your ride requirements.'
        });
        break;

      case 'NO_DRIVER_WITH_LOCATION':
        this.messageService.add({
          severity: 'warn',
          summary: 'Location unavailable',
          detail: 'Drivers are active but none have a valid location.'
        });
        break;

      default:
        this.messageService.add({
          severity: 'error',
          summary: 'Ride rejected',
          detail: 'Ride cannot be created at the moment.'
        });
    }
  }

}


