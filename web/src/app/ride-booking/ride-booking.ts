import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RideBookingService, Location } from '../services/ride-booking.service';
import { FavoriteRoutesService, FavoriteRoute } from '../services/favorite-routes.service';
import { Subject, takeUntil } from 'rxjs';

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

  showRideInfo = false;
  estimatedTime = '';
  estimatedPrice = '';

  showFavorites = false;
  favorites!: ReturnType<FavoriteRoutesService['getFavorites']>;

  private stopIdCounter = 0;
  private searchTimeout: any = null;

  constructor(
    private fb: FormBuilder,
    private rideBookingService: RideBookingService,
    private favoriteService: FavoriteRoutesService
  ) {
    this.initForm();
  }


  initForm(): void {
    this.rideForm = this.fb.group({
      pickupLocation: ['', Validators.required],
      destination: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.favorites = this.favoriteService.getFavorites();

    this.rideBookingService.rideBookingData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.pickup) {
          this.rideForm.patchValue({ pickupLocation: data.pickup.name });
        }
        if (data.destination) {
          this.rideForm.patchValue({ destination: data.destination.name });
        }

        // sync stops
        if (data.stops.length > this.stops.length) {
          const diff = data.stops.length - this.stops.length;
          for (let i = 0; i < diff; i++) {
            this.stops.push({
              id: this.stopIdCounter++,
              location: '',
              suggestions: [],
              showSuggestions: false
            });
          }
        }

        data.stops.forEach((stop, index) => {
          if (this.stops[index]) {
            this.stops[index].location = stop.name;
          }
        });

        this.checkAndUpdateRideInfo(data);
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


  private checkAndUpdateRideInfo(data: any): void {
    if (data.pickup && data.destination) {
      this.showRideInfo = true;
      this.calculateEstimate(data);
    } else {
      this.showRideInfo = false;
    }
  }

  private calculateEstimate(data: any): void {
    const baseTime = 5;
    const timePerStop = 3;
    const totalTime = baseTime + (data.stops.length * timePerStop);

    const basePricePerKm = 50;
    const estimatedKm = 5 + (data.stops.length * 2);
    const totalPrice = Math.round(basePricePerKm * estimatedKm);

    this.estimatedTime = `${totalTime} min`;
    this.estimatedPrice = `${totalPrice} din`;
  }


  onPickupInputChange(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);

    const value = this.rideForm.get('pickupLocation')?.value || '';
    if (value.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(value, 'pickup');
      }, 500);
    } else {
      this.showPickupSuggestions = false;
    }
  }

  selectPickupSuggestion(suggestion: any): void {
    this.rideForm.patchValue({ pickupLocation: suggestion.display_name });
    this.showPickupSuggestions = false;

    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };

    this.rideBookingService.setPickupLocation(location);
  }

  onDestinationInputChange(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);

    const value = this.rideForm.get('destination')?.value || '';
    if (value.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(value, 'destination');
      }, 500);
    } else {
      this.showDestinationSuggestions = false;
    }
  }

  selectDestinationSuggestion(suggestion: any): void {
    this.rideForm.patchValue({ destination: suggestion.display_name });
    this.showDestinationSuggestions = false;

    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };

    this.rideBookingService.setDestinationLocation(location);
  }

  onStopInputChange(stopId: number, event: Event): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);

    const stop = this.stops.find(s => s.id === stopId);
    if (!stop) return;

    const input = event.target as HTMLInputElement;
    stop.location = input.value;

    if (stop.location.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(stop.location, 'stop', stopId);
      }, 500);
    } else {
      stop.showSuggestions = false;
    }
  }

  selectStopSuggestion(stopId: number, suggestion: any): void {
    const stop = this.stops.find(s => s.id === stopId);
    if (!stop) return;

    stop.location = suggestion.display_name;
    stop.showSuggestions = false;

    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };

    const index = this.stops.findIndex(s => s.id === stopId);
    this.rideBookingService.updateStopLocation(index, location);
  }

  private searchLocation(
    query: string,
    type: 'pickup' | 'destination' | 'stop',
    stopId?: number
  ): void {
    const searchQuery = `${query}, Novi Sad, Serbia`;
    const url =
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery)}` +
      `&limit=5&countrycodes=rs&bounded=1&viewbox=19.7,45.3,20.0,45.2`;

    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (type === 'pickup') {
          this.pickupSuggestions = data;
          this.showPickupSuggestions = data.length > 0;
        } else if (type === 'destination') {
          this.destinationSuggestions = data;
          this.showDestinationSuggestions = data.length > 0;
        } else if (type === 'stop' && stopId !== undefined) {
          const stop = this.stops.find(s => s.id === stopId);
          if (stop) {
            stop.suggestions = data;
            stop.showSuggestions = data.length > 0;
          }
        }
      })
      .catch(err => console.error('Search error:', err));
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
    if (index !== -1) {
      this.stops = this.stops.filter(s => s.id !== id);
      this.rideBookingService.removeStopLocation(index);
    }
  }


  onBookRide(): void {
    if (this.rideForm.invalid) {
      this.rideForm.markAllAsTouched();
      alert('Please select pickup and destination locations!');
      return;
    }

    const data = this.rideBookingService.getRideBookingData();
    if (data.pickup && data.destination) {
      this.rideBookingService.calculateRoute();
      console.log('Booking ride:', data);
    }
  }

  clearRoute(): void {
    this.rideForm.reset();
    this.stops = [];
    this.showPickupSuggestions = false;
    this.showDestinationSuggestions = false;
    this.showRideInfo = false;
    this.rideBookingService.clearRoute();
  }

  onSchedule(): void {
    alert('Schedule feature coming soon!');
  }

  trackByStopId(index: number, stop: Stop): number {
    return stop.id;
  }
}
