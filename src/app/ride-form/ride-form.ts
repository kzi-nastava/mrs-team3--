import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RideService, Location } from '../services/ride.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-ride-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './ride-form.html',
  styleUrl: './ride-form.css'
})
export class RideFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  showRideInfo = false;
  
  startLocation: string = '';
  endLocation: string = '';
  
  startSuggestions: any[] = [];
  endSuggestions: any[] = [];
  showStartSuggestions = false;
  showEndSuggestions = false;
  
  private searchTimeout: any = null;

  constructor(private rideService: RideService) {}

  ngOnInit(): void {
    // Osluškuj promene iz servisa (kada se klikne na mapu)
    this.rideService.rideData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.start) {
          this.startLocation = data.start.name;
        }
        if (data.end) {
          this.endLocation = data.end.name;
        }
        
        // Prikaži ride info ako su obe lokacije postavljene
        this.showRideInfo = data.start !== null && data.end !== null;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onStartInputChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
    if (this.startLocation.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(this.startLocation, 'start');
      }, 500);
    } else {
      this.showStartSuggestions = false;
    }
  }

  onEndInputChange(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    
    if (this.endLocation.length > 2) {
      this.searchTimeout = setTimeout(() => {
        this.searchLocation(this.endLocation, 'end');
      }, 500);
    } else {
      this.showEndSuggestions = false;
    }
  }

  private searchLocation(query: string, type: 'start' | 'end'): void {
    const searchQuery = `${query}, Novi Sad, Serbia`;
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery)}&limit=5&countrycodes=rs&bounded=1&viewbox=19.7,45.3,20.0,45.2`;
    
    fetch(url)
      .then(response => response.json())
      .then(data => {
        if (type === 'start') {
          this.startSuggestions = data;
          this.showStartSuggestions = data.length > 0;
        } else {
          this.endSuggestions = data;
          this.showEndSuggestions = data.length > 0;
        }
      })
      .catch(error => console.error('Search error:', error));
  }

  selectStartSuggestion(suggestion: any): void {
    this.startLocation = suggestion.display_name;
    this.showStartSuggestions = false;
    
    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };
    
    this.rideService.setStartLocation(location);
  }

  selectEndSuggestion(suggestion: any): void {
    this.endLocation = suggestion.display_name;
    this.showEndSuggestions = false;
    
    const location: Location = {
      lat: parseFloat(suggestion.lat),
      lng: parseFloat(suggestion.lon),
      name: suggestion.display_name
    };
    
    this.rideService.setEndLocation(location);
  }

  onCalculate(): void {
    const rideData = this.rideService.getRideData();
    
    if (rideData.start && rideData.end) {
      this.rideService.calculateRoute();
      this.showRideInfo = true;
    } else {
      alert('Please select start and end locations!');
    }
  }

  clearRoute(): void {
    this.startLocation = '';
    this.endLocation = '';
    this.showRideInfo = false;
    this.rideService.clearRoute();
  }
}