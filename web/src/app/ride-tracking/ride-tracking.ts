import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RideTrackingService, RideTrackingData, Location } from '../services/ride-tracking.service';
import { AuthService } from '../services/auth.service';
import * as L from 'leaflet';
import { env } from '../../env/env';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

@Component({
  selector: 'app-ride-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ride-tracking.html',
  styleUrls: ['./ride-tracking.css']
})
export class RideTrackingComponent implements OnInit, OnDestroy {
  protected rideData = signal<RideTrackingData | null>(null);
  protected loading = signal<boolean>(true);
  protected error = signal<string | null>(null);

  // For guest mode
  protected trackingToken: string | null = null;
  protected isGuestMode = false;

  // Report modal
  protected showReportModal = signal<boolean>(false);
  protected reportText = '';
  protected reportSubmitting = false;

  // Toast notifications
  protected toasts: Toast[] = [];
  private toastIdCounter = 0;

  // Map
  private map: any = null;
  private markers: any[] = [];
  private routeLines: any[] = [];
  private driverMarker: any = null;
  private routeRequestId = 0;

  constructor(
    private rideTrackingService: RideTrackingService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.trackingToken = params['token'];
      this.isGuestMode = !!this.trackingToken;

      if (this.isGuestMode) {
        this.validateAndLoadGuestRide();
      } else {
        this.loadCurrentRide();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private validateAndLoadGuestRide(): void {
    if (!this.trackingToken) return;

    this.rideTrackingService.validateToken(this.trackingToken).subscribe({
      next: (validation) => {
        if (validation.valid) {
          this.loadGuestRide();
        } else {
          this.error.set(validation.message);
          this.loading.set(false);
        }
      },
      error: (err) => {
        console.error('Token validation error:', err);
        this.error.set('Invalid or expired tracking link');
        this.loading.set(false);
      }
    });
  }

  private loadGuestRide(): void {
    if (!this.trackingToken) return;

    this.rideTrackingService.getRideByToken(this.trackingToken).subscribe({
      next: (data) => {
        this.rideData.set(data);
        this.loading.set(false);
        this.error.set(null);

        setTimeout(() => this.initMap(), 100);
      },
      error: (err) => {
        console.error('Error loading guest ride:', err);
        this.error.set('Failed to load ride information');
        this.loading.set(false);
      }
    });
  }

  private loadCurrentRide(): void {
    this.rideTrackingService.getCurrentRide().subscribe({
      next: (data) => {
        this.rideData.set(data);
        this.loading.set(false);
        this.error.set(null);

        setTimeout(() => this.initMap(), 100);
      },
      error: (err) => {
        console.error('Error loading current ride:', err);

        if (err.status === 204) {
          this.rideData.set(null);
          this.error.set(null);
        } else {
          this.error.set('Failed to load ride information');
        }

        this.loading.set(false);
      }
    });
  }

  protected refreshRide(): void {
    this.loading.set(true);

    if (this.isGuestMode && this.trackingToken) {
      this.loadGuestRide();
    } else {
      this.loadCurrentRide();
    }
  }

  private initMap(): void {
    const ride = this.rideData();
    if (!ride) return;

    const mapElement = document.getElementById('trackingMap');
    if (!mapElement) {
      console.error('Map element not found!');
      return;
    }

    if (this.map) {
      this.map.remove();
    }

    this.map = L.map('trackingMap').setView(
      [ride.startLocation.lat, ride.startLocation.lng],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    this.addMarkers(ride);
    this.drawRoute(ride);
  }

  private addMarkers(ride: RideTrackingData): void {
    this.markers.forEach(m => this.map.removeLayer(m));
    this.markers = [];

    const startMarker = L.marker(
      [ride.startLocation.lat, ride.startLocation.lng],
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
    ).addTo(this.map).bindPopup('Start: ' + ride.startLocation.address);
    this.markers.push(startMarker);

    ride.stops.forEach((stop, index) => {
      const stopMarker = L.marker(
        [stop.lat, stop.lng],
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
      this.markers.push(stopMarker);
    });

    const endMarker = L.marker(
      [ride.endLocation.lat, ride.endLocation.lng],
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
    ).addTo(this.map).bindPopup('Destination: ' + ride.endLocation.address);
    this.markers.push(endMarker);

    if (ride.driverCurrentLocation) {
      if (this.driverMarker) {
        this.map.removeLayer(this.driverMarker);
      }

      const carIcon = L.divIcon({
        className: 'car-marker',
        html: '<div style="font-size: 30px;">🚗</div>',
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });

      this.driverMarker = L.marker(
        [ride.driverCurrentLocation.lat, ride.driverCurrentLocation.lng],
        { icon: carIcon }
      ).addTo(this.map).bindPopup(`Driver: ${ride.driverName}`);
    }

    if (this.markers.length > 0) {
      const group = L.featureGroup(this.markers);
      this.map.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRoute(ride: RideTrackingData): void {
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    this.routeLines.forEach(l => this.map.removeLayer(l));
    this.routeLines = [];

    const waypoints: Location[] = [
      ride.startLocation,
      ...ride.stops,
      ride.endLocation
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1], requestId);
    }
  }

  private drawRouteBetween(start: Location, end: Location, requestId: number): void {
    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.lng},${start.lat}&end=${end.lng},${end.lat}`;

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

        this.routeLines.push(routeLine);
      })
      .catch(err => console.error('Error drawing route:', err));
  }

  protected openReportModal(): void {
    const ride = this.rideData();
    if (!ride || ride.driverName === 'Not assigned') {
      this.showToast('Cannot report: No driver assigned yet', 'warning');
      return;
    }

    this.showReportModal.set(true);
    this.reportText = '';
  }

  protected closeReportModal(): void {
    this.showReportModal.set(false);
    this.reportText = '';
  }

  protected submitReport(): void {
    if (!this.reportText.trim()) {
      this.showToast('Please enter a report description', 'warning');
      return;
    }

    this.reportSubmitting = true;

    const reportObservable = this.isGuestMode && this.trackingToken
      ? this.rideTrackingService.reportInconsistencyByToken(this.trackingToken, this.reportText)
      : this.rideTrackingService.reportInconsistencyForCurrentRide(this.reportText);

    reportObservable.subscribe({
      next: (response) => {
        console.log('Report submitted:', response);
        this.showToast('Report submitted successfully!', 'success');
        this.closeReportModal();
        this.reportSubmitting = false;
      },
      error: (err) => {
        console.error('Error submitting report:', err);
        this.showToast('Failed to submit report. Please try again.', 'error');
        this.reportSubmitting = false;
      }
    });
  }

  protected handlePanic(): void {
    const ride = this.rideData();
    if (!ride) {
      this.showToast('No ride loaded', 'warning');
      return;
    }

    if(ride.status !== 'IN_PROGRESS' && ride.status !== 'PANIC') {
      this.showToast('Panic can only be triggered during an active ride', 'warning');
      return;
    }

    const req$ = (this.isGuestMode && this.trackingToken)
      ? this.rideTrackingService.panicByToken(this.trackingToken)
      : this.rideTrackingService.panic(ride.rideId);

    req$.subscribe({
      next: () => {
        this.showToast('Panic alert sent successfully!', 'success');
        this.refreshRide();
      },
      error: (err) => {
        console.error('Error sending panic alert:', err);
        this.showToast('Failed to send panic alert. Please try again.', 'error');
      }
    });
  }


  // Toast notification methods
  protected showToast(message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info'): void {
    const toast: Toast = {
      id: this.toastIdCounter++,
      message,
      type
    };

    this.toasts.push(toast);

    setTimeout(() => {
      this.removeToast(toast.id);
    }, 5000);
  }

  protected removeToast(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  protected getStatusText(status: string): string {
    switch (status) {
      case 'PENDING': return 'Pending';
      case 'ACCEPTED': return 'Accepted';
      case 'IN_PROGRESS': return 'In Progress';
      case 'COMPLETED': return 'Completed';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  }

  protected getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'ACCEPTED': return 'status-accepted';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'COMPLETED': return 'status-completed';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  protected formatTime(minutes: number): string {
    if (minutes < 1) return 'Less than 1 min';
    if (minutes >= 999999) return 'Calculating...';
    if (minutes < 60) return `${minutes} min`;

    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
  }
}
