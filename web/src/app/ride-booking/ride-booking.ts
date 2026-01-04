import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RideBookingService, Location } from '../services/ride-booking.service';
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
  
  // Reactive Form
  rideForm!: FormGroup;
  
  stops: Stop[] = [];
  
  pickupSuggestions: any[] = [];
  destinationSuggestions: any[] = [];
  showPickupSuggestions = false;
  showDestinationSuggestions = false;
  
  showRideInfo = false;
  estimatedTime: string = '';
  estimatedPrice: string = '';
  
  showFavorites = false;
  favoriteRoutes: any[] = [];
  
  private stopIdCounter = 0;
  private searchTimeout: any = null;

  constructor(
    private fb: FormBuilder,
    private rideBookingService: RideBookingService
  ) {
    this.initForm();
  }

  // Inicijalizacija forme
  initForm(): void {
    this.rideForm = this.fb.group({
      pickupLocation: ['', Validators.required],
      destination: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Listen for ride booking data changes from service
    this.rideBookingService.rideBookingData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.pickup) {
          this.rideForm.patchValue({
            pickupLocation: data.pickup.name
          });
        }
        if (data.destination) {
          this.rideForm.patchValue({
            destination: data.destination.name
          });
        }
        // Update stops from service
        if (data.stops.length > this.stops.length) {
          // Added more stops than we have
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
        // Update stop locations
        data.stops.forEach((stop, index) => {
          if (this.stops[index]) {
            this.stops[index].location = stop.name;
          }
        });
        
        // Check and update ride info display automatically
        this.checkAndUpdateRideInfo(data);
      });

    // Subscribe to form value changes for autocomplete
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

  private checkAndUpdateRideInfo(data: any): void {
    // Show ride info only if we have pickup and destination
    if (data.pickup && data.destination) {
      this.showRideInfo = true;
      this.calculateEstimate(data);
    } else {
      this.showRideInfo = false;
    }
  }

  //Mock calculation of time and price estimates
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

  // Pickup autocomplete
  onPickupInputChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
    const pickupValue = this.rideForm.get('pickupLocation')?.value || '';
    
    if (pickupValue.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(pickupValue, 'pickup');
      }, 500);
    } else {
      this.showPickupSuggestions = false;
    }
  }

  selectPickupSuggestion(suggestion: any): void {
    this.rideForm.patchValue({
      pickupLocation: suggestion.display_name
    });
    this.showPickupSuggestions = false;
    
    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };
    
    this.rideBookingService.setPickupLocation(location);
  }

  // Destination autocomplete
  onDestinationInputChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
    const destinationValue = this.rideForm.get('destination')?.value || '';
    
    if (destinationValue.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(destinationValue, 'destination');
      }, 500);
    } else {
      this.showDestinationSuggestions = false;
    }
  }

  selectDestinationSuggestion(suggestion: any): void {
    this.rideForm.patchValue({
      destination: suggestion.display_name
    });
    this.showDestinationSuggestions = false;
    
    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };
    
    this.rideBookingService.setDestinationLocation(location);
  }

  // Stop autocomplete
  onStopInputChange(stopId: number, event: Event): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
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

  // Generic search location
  private searchLocation(query: string, type: 'pickup' | 'destination' | 'stop', stopId?: number): void {
    const searchQuery = `${query}, Novi Sad, Serbia`;
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery)}&limit=5&countrycodes=rs&bounded=1&viewbox=19.7,45.3,20.0,45.2`;
    
    fetch(url)
      .then(response => response.json())
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
      .catch(error => console.error('Search error:', error));
  }

  addStop(): void {
    const newStop: Stop = {
      id: this.stopIdCounter++,
      location: '',
      suggestions: [],
      showSuggestions: false
    };
    this.stops.push(newStop);
  }

  removeStop(id: number): void {
    const index = this.stops.findIndex(s => s.id === id);
    if (index !== -1) {
      this.stops = this.stops.filter(stop => stop.id !== id);
      this.rideBookingService.removeStopLocation(index);
    }
  }

  onBookRide(): void {
    if (this.rideForm.invalid) {
      this.rideForm.markAllAsTouched();
      alert('Please select pickup and destination locations!');
      return;
    }

    const rideData = this.rideBookingService.getRideBookingData();
    
    if (rideData.pickup && rideData.destination) {
      this.rideBookingService.calculateRoute();
      console.log('Booking ride:', rideData);
    } else {
      alert('Please select pickup and destination locations!');
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

  // Feature for scheduling rides 
  onSchedule(): void {
    console.log('Schedule for later clicked');
    alert('Schedule feature coming soon!');
  }

  toggleFavorites(): void {
    this.showFavorites = !this.showFavorites;
    
    //Mock data for testing
    if (this.favoriteRoutes.length === 0) {
      this.favoriteRoutes = [
        {
          id: 1,
          name: 'Home to Work',
          pickup: 'Bulevar oslobođenja 46, Novi Sad',
          destination: 'Faculty of Technical Sciences, Novi Sad'
        },
        {
          id: 2,
          name: 'Weekend Trip',
          pickup: 'Novi Sad',
          destination: 'Belgrade Airport'
        }
      ];
    }
  }

  useFavoriteRoute(route: any): void {
    this.rideForm.patchValue({
      pickupLocation: route.pickup,
      destination: route.destination
    });
    
    console.log('Using favorite route:', route);
    this.showFavorites = false;
  }

  deleteFavoriteRoute(id: number): void {
    this.favoriteRoutes = this.favoriteRoutes.filter(r => r.id !== id);
  }

  trackByStopId(index: number, stop: Stop): number {
    return stop.id;
  }
}