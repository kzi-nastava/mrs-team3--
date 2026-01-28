import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DriverRideService, DriverRide, PendingRide, Location } from '../services/driver-ride.service';
import * as L from 'leaflet';
import { env } from '../../env/env';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

@Component({
  selector: 'app-driver-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-dashboard.html',
  styleUrls: ['./driver-dashboard.css']
})
export class DriverDashboardComponent implements OnInit, OnDestroy {
  protected myRides = signal<DriverRide[]>([]);
  protected pendingRides = signal<PendingRide[]>([]);
  protected selectedRide = signal<DriverRide | PendingRide | null>(null);
  protected loading = signal<boolean>(true);

  // Map
  private map: any = null;
  private markers: any[] = [];
  private routeLines: any[] = [];
  private driverMarker: any = null;

  // Modals
  protected showFinishModal = signal<boolean>(false);

  protected showCancelModal = signal<boolean>(false);
  protected cancelReason = signal<string>('');


  // Toast notifications
  protected toasts: Toast[] = [];
  private toastIdCounter = 0;

  constructor(private service: DriverRideService) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.clearMap();
  }

  private loadData(): void {
    this.loading.set(true);

    this.service.getMyRides().subscribe({
      next: (rides) => {
        this.myRides.set(rides);
        this.loading.set(false);

        if (rides.length > 0) {
          this.selectRide(rides[0]);
        } else {
          this.loadPendingRides();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.loadPendingRides();
        this.showToast('Failed to load rides', 'error');
      }
    });
  }

  private loadPendingRides(): void {
    this.service.getPendingRides().subscribe({
      next: (rides) => {
        this.pendingRides.set(rides);
      },
      error: () => {
        this.showToast('Failed to load pending rides', 'error');
      }
    });
  }

  protected selectRide(ride: DriverRide | PendingRide): void {
    this.selectedRide.set(ride);
    setTimeout(() => this.initMap(), 100);
  }

  protected acceptRide(ride: PendingRide): void {
    this.service.acceptRide(ride.rideId).subscribe({
      next: () => {
        this.showToast('Ride accepted successfully!', 'success');
        this.loadData();
      },
      error: (err) => {
        this.showToast('Failed to accept ride: ' + (err.error?.message || 'Unknown error'), 'error');
      }
    });
  }

  protected moveToStart(): void {
    this.service.moveToStart().subscribe({
      next: () => {
        const ride = this.selectedRide() as DriverRide;
        this.updateDriverMarker(ride.startLocation);
        this.showToast('Moved to pickup location', 'success');
      },
      error: () => {
        this.showToast('Failed to move to start', 'error');
      }
    });
  }

  protected startRide(): void {
    const ride = this.selectedRide() as DriverRide;
    this.service.startRide(ride.rideId).subscribe({
      next: () => {
        this.showToast('Ride started!', 'success');
        this.loadData();
      },
      error: () => {
        this.showToast('Failed to start ride', 'error');
      }
    });
  }

  protected openFinishModal(): void {
    this.showFinishModal.set(true);
  }

  protected closeFinishModal(): void {
    this.showFinishModal.set(false);
  }

  protected finishRide(): void {
    const ride = this.selectedRide() as DriverRide;

    // FIXED: Pass the actual rideId
    this.service.finishRide(ride.rideId, null).subscribe({
      next: (response) => {
        this.closeFinishModal();
        
        // Clear the map and selected ride to prevent residue
        this.clearMap();
        this.selectedRide.set(null);
        
        if (response.hasNextRide) {
          this.showToast('Ride completed! Your next ride is now active.', 'success');
        } else {
          this.showToast('Ride completed! You are now available for new rides.', 'success');
        }

        this.loadData();
      },
      error: (err) => {
        console.error('Finish ride error:', err);
        this.showToast('Failed to finish ride: ' + (err.error?.message || 'Unknown error'), 'error');
      }
    });
  }

  protected markStopReached(stopIndex: number): void {
    this.service.reachStop(stopIndex).subscribe({
      next: (updatedRide) => {
        this.selectedRide.set(updatedRide);
        this.showToast(`Stop ${stopIndex + 1} marked as reached`, 'success');
        
        // Update the markers and redraw route to reflect the new state
        this.addMarkers(updatedRide);
        this.drawRoute(updatedRide);
      },
      error: () => {
        this.showToast('Failed to mark stop as reached', 'error');
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

  // Map Methods

  private initMap(): void {
    const ride = this.selectedRide();
    if (!ride) return;

    const mapElement = document.getElementById('driverMap');
    if (!mapElement) return;

    if (this.map) this.map.remove();

    this.map = L.map('driverMap').setView(
      [ride.startLocation.lat, ride.startLocation.lng],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    // For IN_PROGRESS rides, allow clicking to move
    if (this.isDriverRide(ride) && ride.status === 'IN_PROGRESS') {
      this.map.on('click', (e: any) => {
        this.moveDriverTo(e.latlng);
      });
    }

    this.addMarkers(ride);
    this.drawRoute(ride);
  }

  private addMarkers(ride: DriverRide | PendingRide): void {
    this.markers.forEach(m => this.map.removeLayer(m));
    this.markers = [];

    // Start marker (green)
    const startMarker = L.marker([ride.startLocation.lat, ride.startLocation.lng], {
      icon: this.getIcon('green')
    }).addTo(this.map).bindPopup('Pickup: ' + ride.startLocation.address);
    this.markers.push(startMarker);

    // Stop markers (blue for unreached, gold for reached)
    ride.stops.forEach((stop, index) => {
      const reached = this.isDriverRide(ride)
        ? ride.stopStatuses[index]?.reached
        : false;

      const color = reached ? 'gold' : 'blue';
      const label = reached ? `Stop ${index + 1} ✓` : `Stop ${index + 1}`;

      const stopMarker = L.marker([stop.lat, stop.lng], {
        icon: this.getIcon(color)
      }).addTo(this.map).bindPopup(label);
      this.markers.push(stopMarker);
    });

    // End marker (red)
    const endMarker = L.marker([ride.endLocation.lat, ride.endLocation.lng], {
      icon: this.getIcon('red')
    }).addTo(this.map).bindPopup('Destination: ' + ride.endLocation.address);
    this.markers.push(endMarker);

    // Driver marker for IN_PROGRESS
    if (this.isDriverRide(ride) && ride.status === 'IN_PROGRESS') {
      this.driverMarker = L.marker([ride.startLocation.lat, ride.startLocation.lng], {
        icon: L.divIcon({
          className: 'car-marker',
          html: '<div style="font-size: 30px;">🚗</div>',
          iconSize: [30, 30],
          iconAnchor: [15, 15]
        })
      }).addTo(this.map).bindPopup('You are here');
    }

    if (this.markers.length > 0) {
      const group = L.featureGroup(this.markers);
      this.map.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRoute(ride: DriverRide | PendingRide): void {
    this.routeLines.forEach(l => this.map.removeLayer(l));
    this.routeLines = [];

    let waypoints: Location[];

    // For in-progress rides, only draw route from current position through unreached stops
    if (this.isDriverRide(ride) && ride.status === 'IN_PROGRESS') {
      // Get unreached stops only
      const unreachedStops = ride.stops.filter((stop, index) => 
        !ride.stopStatuses[index]?.reached
      );

      // Start from driver's current position (or start location as fallback)
      const currentPosition = this.driverMarker 
        ? { lat: this.driverMarker.getLatLng().lat, lng: this.driverMarker.getLatLng().lng, address: 'Current Position' }
        : ride.startLocation;

      waypoints = [currentPosition, ...unreachedStops, ride.endLocation];
    } else {
      // For pending/accepted rides, show full route
      waypoints = [ride.startLocation, ...ride.stops, ride.endLocation];
    }

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1]);
    }
  }

  private drawRouteBetween(start: Location, end: Location): void {
    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.lng},${start.lat}&end=${end.lng},${end.lat}`;

    fetch(url)
      .then(r => r.json())
      .then(data => {
        const coords = data.features[0].geometry.coordinates;
        const routeCoords = coords.map((c: any) => [c[1], c[0]]);

        const routeLine = L.polyline(routeCoords, {
          color: '#6366f1',
          weight: 4,
          opacity: 0.7
        }).addTo(this.map);

        this.routeLines.push(routeLine);
      })
      .catch(() => {
        this.showToast('Failed to load route', 'warning');
      });
  }

  private moveDriverTo(latlng: any): void {
    if (!this.driverMarker) return;

    this.driverMarker.setLatLng(latlng);

    this.service.updateLocation(latlng.lat, latlng.lng).subscribe({
      next: () => {
        // Redraw route from new position to update remaining distance/time
        const ride = this.selectedRide();
        if (ride && this.isDriverRide(ride)) {
          this.drawRoute(ride);
        }
      },
      error: () => {
        this.showToast('Failed to update location', 'error');
      }
    });
  }

  private updateDriverMarker(location: Location): void {
    if (this.driverMarker) {
      this.driverMarker.setLatLng([location.lat, location.lng]);
      this.map.setView([location.lat, location.lng], 15);
    }
  }

  private getIcon(color: string): any {
    const colorMap: any = {
      green: 'marker-icon-2x-green.png',
      blue: 'marker-icon-2x-blue.png',
      red: 'marker-icon-2x-red.png',
      gold: 'marker-icon-2x-gold.png'
    };

    return L.icon({
      iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/${colorMap[color]}`,
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });
  }

  protected isDriverRide(ride: any): ride is DriverRide {
    return 'status' in ride && 'stopStatuses' in ride;
  }

  protected refreshRides(): void {
    this.loadData();
  }

  protected openCancelModal(): void {
    const ride = this.selectedRide();
    if (!ride || !this.isDriverRide(ride)) return;

    if (ride.status !== 'ACCEPTED') {
      this.showToast('Ride cannot be cancelled at this stage', 'warning');
      return;
    }

    this.cancelReason.set('');
    this.showCancelModal.set(true);
  }

  protected closeCancelModal(): void {
    this.showCancelModal.set(false);
  }

  protected confirmCancelRide(): void {
    const ride = this.selectedRide();
    if (!ride || !this.isDriverRide(ride)) return;

    if (ride.status !== 'ACCEPTED') {
      this.showToast('Ride cannot be cancelled at this stage', 'warning');
      return;
    }

    const reason = this.cancelReason().trim();
    if (!reason) return;

    this.service.cancelRide(ride.rideId, reason).subscribe({
      next: () => {
        this.showToast('Ride cancelled successfully', 'success');
        this.showCancelModal.set(false);
        this.loadData();
      },
      error: (err) => {
        this.showToast('Failed to cancel ride: ' + (err.error?.message || 'Unknown error'), 'error');
      }
    });
  }
}
  private clearMap(): void {
    // Remove all markers
    this.markers.forEach(m => {
      if (this.map) this.map.removeLayer(m);
    });
    this.markers = [];

    // Remove driver marker
    if (this.driverMarker && this.map) {
      this.map.removeLayer(this.driverMarker);
      this.driverMarker = null;
    }

    // Remove route lines
    this.routeLines.forEach(l => {
      if (this.map) this.map.removeLayer(l);
    });
    this.routeLines = [];

    // Remove the map entirely
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }
}
