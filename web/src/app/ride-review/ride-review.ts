import { Component, OnInit, AfterViewInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RideReviewService, RideReviewDetail } from '../services/ride-review.service';
import { AuthService } from '../services/auth.service';
import * as L from 'leaflet';
import { env } from '../../env/env';

@Component({
  selector: 'app-ride-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ride-review.html',
  styleUrls: ['./ride-review.css'],
})
export class RideReviewComponent implements OnInit, AfterViewInit {

  protected rideDetail = signal<RideReviewDetail | null>(null);
  protected errorMessage = signal<string>('');
  protected successMessage = signal<string>('');
  protected loading = signal<boolean>(true);
  
  // Review form fields
  driverRating: number = 0;
  vehicleRating: number = 0;
  comment: string = '';
  
  // Map
  private map: any = null;
  private mapMarkers: any[] = [];
  private mapRouteLines: any[] = [];
  private routeRequestId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reviewService: RideReviewService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const rideId = this.route.snapshot.paramMap.get('id');
    
    if (!rideId) {
      this.errorMessage.set('No ride ID provided');
      this.loading.set(false);
      return;
    }
    
    this.loadRideDetails(+rideId);
  }

  ngAfterViewInit(): void {
    // Map will be initialized after data loads
  }

  private loadRideDetails(rideId: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    
    this.reviewService.getRideForReview(rideId).subscribe({
      next: (detail) => {
        this.rideDetail.set(detail);
        
        // If there's an existing review, populate the form
        if (detail.existingReview) {
          this.driverRating = detail.existingReview.driverRating;
          this.vehicleRating = detail.existingReview.vehicleRating;
          this.comment = detail.existingReview.comment || '';
        }
        
        this.loading.set(false);
        
        // Initialize map after data is loaded
        setTimeout(() => {
          this.initMap(detail);
        }, 100);
      },
      error: (err) => {
        console.error('Error loading ride details:', err);
        this.errorMessage.set(
          err.error?.message || 
          'Unable to load ride details. You may not have access to this ride.'
        );
        this.loading.set(false);
      }
    });
  }

  private initMap(detail: RideReviewDetail): void {
    if (this.map) {
      this.map.remove();
    }

    const mapElement = document.getElementById('reviewMap');
    if (!mapElement) {
      console.error('Map element not found!');
      return;
    }

    this.map = L.map('reviewMap').setView(
      [detail.startLocation.latitude, detail.startLocation.longitude],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    // Add start marker
    const startMarker = L.marker(
      [detail.startLocation.latitude, detail.startLocation.longitude],
      {
        icon: L.icon({
          iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
          shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41]
        })
      }
    ).addTo(this.map).bindPopup('Pickup: ' + detail.startLocation.address);
    this.mapMarkers.push(startMarker);

    // Add stop markers
    detail.stops.forEach((stop, index) => {
      const stopMarker = L.marker(
        [stop.latitude, stop.longitude],
        {
          icon: L.icon({
            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
          })
        }
      ).addTo(this.map).bindPopup(`Stop ${index + 1}: ${stop.address}`);
      this.mapMarkers.push(stopMarker);
    });

    // Add end marker
    const endMarker = L.marker(
      [detail.endLocation.latitude, detail.endLocation.longitude],
      {
        icon: L.icon({
          iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
          shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41]
        })
      }
    ).addTo(this.map).bindPopup('Destination: ' + detail.endLocation.address);
    this.mapMarkers.push(endMarker);

    this.drawRoute(detail);
  }

  private drawRoute(detail: RideReviewDetail): void {
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    const waypoints = [
      detail.startLocation,
      ...detail.stops,
      detail.endLocation
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1], requestId);
    }

    if (this.mapMarkers.length > 0) {
      const group = L.featureGroup(this.mapMarkers);
      this.map.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRouteBetween(start: any, end: any, requestId: number): void {
    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}`;

    fetch(url)
      .then(r => r.json())
      .then(data => {
        if (requestId !== this.routeRequestId) return;

        const coords = data.features[0].geometry.coordinates;
        const routeCoords = coords.map((c: any) => [c[1], c[0]]);

        const routeLine = L.polyline(routeCoords, {
          color: '#6366f1',
          weight: 4,
          opacity: 0.7
        }).addTo(this.map);

        this.mapRouteLines.push(routeLine);
      })
      .catch(err => {
        console.error('Error drawing route:', err);
      });
  }

  protected setRating(type: 'driver' | 'vehicle', rating: number): void {
    if (type === 'driver') {
      this.driverRating = rating;
    } else {
      this.vehicleRating = rating;
    }
  }

  protected submitReview(): void {
    const detail = this.rideDetail();
    if (!detail) return;

    // Validate ratings
    if (this.driverRating === 0 || this.vehicleRating === 0) {
      this.errorMessage.set('Please provide both driver and vehicle ratings');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.reviewService.submitReview(detail.rideId, {
      driverRating: this.driverRating,
      vehicleRating: this.vehicleRating,
      comment: this.comment || null
    }).subscribe({
      next: (response) => {
        this.successMessage.set(response.message);
        this.loading.set(false);
        
        // Reload the ride details to update the existing review
        setTimeout(() => {
          this.loadRideDetails(detail.rideId);
        }, 1500);
      },
      error: (err) => {
        console.error('Error submitting review:', err);
        this.errorMessage.set(
          err.error?.message || 'Failed to submit review. Please try again.'
        );
        this.loading.set(false);
      }
    });
  }

  protected goBack(): void {
    this.router.navigate(['/passenger-history']);
  }

  protected formatDateTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  protected getStarArray(max: number): number[] {
    return Array.from({ length: max }, (_, i) => i + 1);
  }

  protected getDaysRemaining(): number {
    const detail = this.rideDetail();
    if (!detail) return 0;
    
    const now = new Date();
    const deadline = new Date(detail.reviewDeadline);
    const diffTime = deadline.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    return Math.max(0, diffDays);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }
}