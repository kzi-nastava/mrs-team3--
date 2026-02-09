import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminRideTrackingService, AdminRideTrackingData, Location } from '../services/admin-ride-tracking.service';
import * as L from 'leaflet';
import { env } from '../../env/env';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

@Component({
  selector: 'app-admin-ride-tracking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-ride-tracking.html',
  styleUrls: ['./admin-ride-tracking.css']
})
export class AdminRideTrackingComponent implements OnInit, OnDestroy {
  protected rideData = signal<AdminRideTrackingData | null>(null);
  protected loading = signal<boolean>(true);
  protected error = signal<string | null>(null);

  private driverId: number | null = null;
  private pollingInterval: any = null;

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
    private service: AdminRideTrackingService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.driverId = +params['driverId'];
      
      if (this.driverId) {
        this.loadRideData();
        this.startPolling();
      } else {
        this.error.set('Invalid driver ID');
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
    if (this.map) {
      this.map.remove();
    }
  }

  private loadRideData(): void {
    if (!this.driverId) return;

    this.service.getRideByDriverId(this.driverId).subscribe({
      next: (data) => {
        this.rideData.set(data);
        this.loading.set(false);
        this.error.set(null);

        setTimeout(() => this.initMap(), 100);
      },
      error: (err) => {
        console.error('Error loading ride data:', err);

        if (err.status === 204 || err.status === 404) {
          this.rideData.set(null);
          this.error.set(null);
        } else {
          this.error.set('Failed to load ride information');
        }

        this.loading.set(false);
      }
    });
  }

  private startPolling(): void {
    // Poll every 20 seconds
    this.pollingInterval = setInterval(() => {
      this.loadRideDataSilently();
    }, 20000);
  }

  private stopPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  private loadRideDataSilently(): void {
    if (!this.driverId) return;

    this.service.getRideByDriverId(this.driverId).subscribe({
      next: (data) => {
        this.rideData.set(data);
        this.updateMap();
      },
      error: (err) => {
        console.error('Polling error:', err);
        // Don't show error toast for polling failures
      }
    });
  }

  protected refreshRide(): void {
    this.loading.set(true);
    this.loadRideData();
  }

  protected goBack(): void {
    this.router.navigate(['/admin/users']);
  }

  private initMap(): void {
    const ride = this.rideData();
    if (!ride) return;

    const mapElement = document.getElementById('adminTrackingMap');
    if (!mapElement) {
      console.error('Map element not found!');
      return;
    }

    if (this.map) {
      this.map.remove();
    }

    this.map = L.map('adminTrackingMap').setView(
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

  private updateMap(): void {
    const ride = this.rideData();
    if (!ride || !this.map) return;

    this.addMarkers(ride);
    this.drawRoute(ride);
  }

  private addMarkers(ride: AdminRideTrackingData): void {
    this.markers.forEach(m => this.map.removeLayer(m));
    this.markers = [];

    // Start marker (green)
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

    // Stop markers with color based on reached status
    ride.stopStatuses.forEach((stopStatus) => {
      const color = stopStatus.reached ? 'gold' : 'blue';
      const label = stopStatus.reached ? `Stop ${stopStatus.stopIndex + 1} ✓` : `Stop ${stopStatus.stopIndex + 1}`;
      
      const stopMarker = L.marker(
        [stopStatus.location.lat, stopStatus.location.lng],
        {
          icon: L.icon({
            iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${color}.png`,
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
          })
        }
      ).addTo(this.map).bindPopup(label);
      this.markers.push(stopMarker);
    });

    // End marker (red)
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

    // Driver marker
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

  private drawRoute(ride: AdminRideTrackingData): void {
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    this.routeLines.forEach(l => this.map.removeLayer(l));
    this.routeLines = [];

    let waypoints: Location[];

    // For in-progress rides, only draw route from driver position through unreached stops
    if (ride.status === 'IN_PROGRESS' && ride.driverCurrentLocation) {
      // Get unreached stops only
      const unreachedStops = ride.stopStatuses
        .filter(s => !s.reached)
        .map(s => s.location);

      waypoints = [ride.driverCurrentLocation, ...unreachedStops, ride.endLocation];
    } else {
      // For pending/accepted rides, show full route
      const allStops = ride.stopStatuses.map(s => s.location);
      waypoints = [ride.startLocation, ...allStops, ride.endLocation];
    }

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
      case 'PANIC': return 'PANIC';
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
      case 'PANIC': return 'status-panic';
      default: return '';
    }
  }

  protected formatTime(minutes: number): string {
    if (minutes >= 999999) return 'Waiting to start';
    if (minutes <= 0) return 'Arrived';
    if (minutes < 60) return `${minutes} min`;

    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  protected formatDateTime(dateString: string | null): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString();
  }
}