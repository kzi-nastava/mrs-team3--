import { Component, signal, computed, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RideHistoryService, Ride } from '../../services/passenger-history.service';
import { FavoriteRoutesService } from '../../services/favorite-routes.service';
import { env } from '../../../env/env';
import * as L from 'leaflet';

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './passenger-history.html',
  styleUrls: ['./passenger-history.css'],
})
export class PassengerHistoryComponent implements OnInit, AfterViewInit {

  protected rides = signal<Ride[]>([]);
  protected selectedRide = signal<Ride | null>(null);

  // Filter states
  startDate: string = '';
  endDate: string = '';

  sortOption: string = 'startTime-desc';

  // Map
  private detailMap: any = null;
  private mapMarkers: any[] = [];
  private mapRouteLines: any[] = [];

  constructor(
    private driverHistoryService: RideHistoryService,
    private favoriteService: FavoriteRoutesService,
  ) {}

  ngOnInit(): void {
    this.loadRides();
  }

  ngAfterViewInit(): void {
    // Map will be initialized when modal opens
  }

  protected filteredRides = computed(() => {
    let rides = this.rides();

    if (this.startDate) {
      const from = new Date(this.startDate);
      from.setHours(0, 0, 0, 0);
      rides = rides.filter(r => new Date(r.startTime) >= from);
    }

    if (this.endDate) {
      const to = new Date(this.endDate);
      to.setHours(23, 59, 59, 999);
      rides = rides.filter(r => new Date(r.startTime) <= to);
    }

    const sorted = [...rides];

    switch (this.sortOption) {
      case 'startTime-asc':
        sorted.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
        break;
      case 'startTime-desc':
        sorted.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
        break;

      case 'endTime-asc':
        sorted.sort((a, b) => this.endMillis(a) - this.endMillis(b));
        break;
      case 'endTime-desc':
        sorted.sort((a, b) => this.endMillis(b) - this.endMillis(a));
        break;

      case 'route-asc':
        sorted.sort((a, b) => this.routeText(a).localeCompare(this.routeText(b)));
        break;
      case 'route-desc':
        sorted.sort((a, b) => this.routeText(b).localeCompare(this.routeText(a)));
        break;
    }

    return sorted;
  });

  protected onSortChange(): void {
    this.rides.set([...this.rides()]);
  }

  private loadRides(): void {
    this.driverHistoryService.rides$.subscribe(rides => {
      // Sync favorite status with favorite routes service
      const favorites = this.favoriteService.getFavorites();
      const updatedRides = rides.map(r => ({
        ...r,
        favorite: favorites().some(f =>
  f.from.address === r.startLocation.address &&
  f.to.address === r.endLocation.address
)

      }));
      this.rides.set(updatedRides);
    });
  }

  protected onFilterChange(): void {
    this.rides.set([...this.rides()]);
  }

  protected openRideDetails(ride: Ride): void {
    this.selectedRide.set(ride);

    setTimeout(() => {
      this.initDetailMap(ride);
    }, 100);
  }

  protected closeModal(event?: Event): void {
    if (event) {
      event?.stopPropagation();
    }
    this.selectedRide.set(null);

    if (this.detailMap) {
      this.detailMap.remove();
      this.detailMap = null;
      this.mapMarkers = [];
      this.mapRouteLines = [];
    }
  }

  private routeText(r: Ride): string {
    return `${r.startLocation.address} -> ${r.endLocation.address}`;
  }

  private endMillis(r: Ride): number {
    return r.endTime ? new Date(r.endTime).getTime() : 0;
  }

  protected toggleFavoriteFromCard(event: Event, ride: Ride): void {
    event.stopPropagation(); // Prevent opening modal
    this.toggleFavorite(ride);
  }

  protected toggleFavorite(ride: Ride): void {

  const route = {
    from: {
      address: ride.startLocation.address,
      latitude: ride.startLocation.latitude,
      longitude: ride.startLocation.longitude
    },
    to: {
      address: ride.endLocation.address,
      latitude: ride.endLocation.latitude,
      longitude: ride.endLocation.longitude
    },
    stops: ride.stops.map(s => ({
      address: s.address,
      latitude: s.latitude,
      longitude: s.longitude
    })),

    vehicleType: ride.vehicleType,
    babyTransport: ride.babyTransport,
    petTransport: ride.petTransport
  };

  if (ride.favorite) {
    this.favoriteService.remove(route);
  } else {
    this.favoriteService.add(route);
  }

  this.rides.set(
    this.rides().map(r =>
      r.id === ride.id ? { ...r, favorite: !r.favorite } : r
    )
  );

  if (this.selectedRide()?.id === ride.id) {
    this.selectedRide.set({ ...ride, favorite: !ride.favorite });
  }
}

  private initDetailMap(ride: Ride): void {
    if (this.detailMap) {
      this.detailMap.remove();
    }

    const mapElement = document.getElementById('detailMap');
    if (!mapElement) return;

    this.detailMap = L.map('detailMap').setView(
      [ride.startLocation.latitude, ride.startLocation.longitude],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.detailMap);

    const startMarker = L.marker(
      [ride.startLocation.latitude, ride.startLocation.longitude],
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
    ).addTo(this.detailMap).bindPopup('Pickup: ' + ride.startLocation.address);
    this.mapMarkers.push(startMarker);

    ride.stops.forEach((stop, index) => {
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
      ).addTo(this.detailMap).bindPopup(`Stop ${index + 1}: ${stop.address}`);
      this.mapMarkers.push(stopMarker);
    });

    const endMarker = L.marker(
      [ride.endLocation.latitude, ride.endLocation.longitude],
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
    ).addTo(this.detailMap).bindPopup('Destination: ' + ride.endLocation.address);
    this.mapMarkers.push(endMarker);

    // Draw route
    this.drawRideRoute(ride);
  }

  private drawRideRoute(ride: Ride): void {
    const waypoints = [
      ride.startLocation,
      ...ride.stops,
      ride.endLocation
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1]);
    }

    // Fit map to show all markers
    if (this.mapMarkers.length > 0) {
      const group = L.featureGroup(this.mapMarkers);
      this.detailMap.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRouteBetween(start: any, end: any): void {
    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}`;

    fetch(url)
      .then(r => r.json())
      .then(data => {
        const coords = data.features[0].geometry.coordinates;
        const routeCoords = coords.map((c: any) => [c[1], c[0]]);

        const routeLine = L.polyline(routeCoords, {
          color: '#6366f1',
          weight: 4,
          opacity: 0.7
        }).addTo(this.detailMap);

        this.mapRouteLines.push(routeLine);
      })
      .catch(err => {
        console.error('Error drawing route:', err);
      });
  }

  protected viewOnMap(ride: Ride): void {
    // This method is no longer needed since map is embedded
    console.log('Map is already showing in modal');
  }

  protected viewReport(): void {
    // TODO: Navigate to reports page or generate PDF
    console.log('View report clicked');
  }

  protected getStatusText(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'Completed';
      case 'CANCELLED_BY_DRIVER':
        return 'Cancelled by Driver';
      case 'CANCELLED_BY_PASSENGER':
        return 'Cancelled by Passenger';
      case 'PANIC':
        return 'Panic';
      default:
        return status;
    }
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
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

  protected bookAgain(event: Event, ride: Ride): void {
    event.stopPropagation();

    // Example: navigate with route data
    // this.router.navigate(['/book'], { state: { ride } });

    console.log('Book again:', ride);
  }

  protected scheduleRide(event: Event, ride: Ride): void {
    event.stopPropagation();

    // Example: navigate to scheduling screen
    // this.router.navigate(['/schedule'], { state: { ride } });

    console.log('Schedule ride:', ride);
  }
}
