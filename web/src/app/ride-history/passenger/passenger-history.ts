import { Component, signal, computed, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { RatingModule } from 'primeng/rating';

import { RideHistoryService } from '../../services/passenger-history.service';
import { FavoriteRoutesService } from '../../services/favorite-routes.service';
import { env } from '../../../env/env';

import {Router} from '@angular/router';
import {RideBookingService} from '../../services/ride-booking.service';

export type VehicleType = 'STANDARD' | 'VAN' | 'LUXURY';

type RideStatus = string;

type Location = {
  lat: number;
  lng: number;
  address: string;
};

type InconsistencyReportItem = {
  id: number;
  reportText: string;
  createdAt: string;
};

export type PassengerRideSummary = {
  id: number;
  status: RideStatus;
  startLocation: Location;
  endLocation: Location;
  startTime: string;
  endTime: string | null;
  favorite: boolean;
};

export type PassengerRideDetails = PassengerRideSummary & {
  stops: Location[];
  driverName: string;
  driverReview: number | null;
  rideReview: number | null;
  inconsistencyReports: InconsistencyReportItem[];
};

export type Ride = {
  id: number;

  startLocation: Location;
  endLocation: Location;
  startTime: string;
  endTime: string | null;
  status: RideStatus;
  favorite: boolean;

  stops: Location[];
  driverName: string;
  driverReview?: number | null;
  rideReview?: number | null;
  inconsistencyReport?: string[];
};

type SortOption =
  | 'startTime-desc'
  | 'startTime-asc'
  | 'endTime-desc'
  | 'endTime-asc'
  | 'route-asc'
  | 'route-desc';

type RideForRouteText = {
  startLocation: { address: string };
  endLocation: { address: string };
};

type RideForEndMillis = {
  endTime?: string | null;
};

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RatingModule],
  templateUrl: './passenger-history.html',
  styleUrls: ['./passenger-history.css'],
})
export class PassengerHistoryComponent implements OnInit, AfterViewInit {
  protected rides = signal<Ride[]>([]);
  protected selectedRide = signal<Ride | null>(null);

  startDate: string = '';
  endDate: string = '';

  sortOption: SortOption = 'startTime-desc';

  private detailMap: L.Map | null = null;
  private mapMarkers: L.Marker[] = [];
  private mapRouteLines: L.Polyline[] = [];
  private routeRequestId = 0;

  constructor(
    private driverHistoryService: RideHistoryService,
    private favoriteService: FavoriteRoutesService,
    private rideBookingService: RideBookingService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRides();
  }

  ngAfterViewInit(): void {
  }

  protected filteredRides = computed(() => {
    let rides = this.rides();

    if (this.startDate) {
      const from = new Date(`${this.startDate}T00:00:00`);
      rides = rides.filter(r => new Date(r.startTime) >= from);
    }

    if (this.endDate) {
      const to = new Date(`${this.endDate}T23:59:59.999`);
      rides = rides.filter(r => new Date(r.startTime) <= to);
    }

    const sorted = [...rides];

    switch (this.sortOption) {
      case 'startTime-asc':
        sorted.sort(
          (a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
        );
        break;
      case 'startTime-desc':
        sorted.sort(
          (a, b) =>
            new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
        );
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

  protected onFilterChange(): void {
    this.rides.set([...this.rides()]);
  }

  private loadRides(): void {
    this.driverHistoryService.getRides().subscribe({
      next: (rows: PassengerRideSummary[]) => {
        const mapped = (rows ?? []).map(r => this.mapSummaryToRide(r));
        this.rides.set(mapped);
      },
      error: err => console.error('getRides failed:', err),
    });
  }

  protected openRideDetails(ride: Ride): void {
    this.driverHistoryService.getRideDetails(ride.id).subscribe({
      next: (details: PassengerRideDetails) => {
        const merged = this.mergeDetails(ride, details);
        this.selectedRide.set(merged);

        setTimeout(() => this.initDetailMap(merged), 400);
      },
      error: err => {
        console.error('getRideDetails failed:', err);
        this.selectedRide.set(ride);
        setTimeout(() => this.initDetailMap(ride), 400);
      },
    });
  }

  protected closeModal(event?: Event): void {
    event?.stopPropagation?.();
    this.selectedRide.set(null);

    if (this.detailMap) {
      this.detailMap.remove();
      this.detailMap = null;
    }
    this.mapMarkers = [];
    this.mapRouteLines = [];
    this.resetLeafletContainer('detailMap');
  }

  protected toggleFavoriteFromCard(event: Event, ride: Ride): void {
    event.stopPropagation();

    this.driverHistoryService.getRideDetails(ride.id).subscribe({
      next: (details: PassengerRideDetails) => {
        const forUi = this.mergeDetails(ride, details);
        this.toggleFavorite(forUi);
      },
      error: err => console.error('toggleFavoriteFromCard -> details failed:', err),
    });
  }

  protected toggleFavorite(ride: Ride): void {
    const req = {
      rideId: ride.id,
      from: {
        address: ride.startLocation.address,
        latitude: Number(ride.startLocation.lat),
        longitude: Number(ride.startLocation.lng),
      },
      to: {
        address: ride.endLocation.address,
        latitude: Number(ride.endLocation.lat),
        longitude: Number(ride.endLocation.lng),
      },
      stops: (ride.stops ?? []).map(s => ({
        address: s.address,
        latitude: Number(s.lat),
        longitude: Number(s.lng),
      })),
      vehicleType: 'STANDARD' as VehicleType,
      babyTransport: false,
      petTransport: false,
    };

    this.favoriteService.toggle(req);

    this.rides.set(
      this.rides().map(r => (r.id === ride.id ? { ...r, favorite: !r.favorite } : r))
    );

    if (this.selectedRide()?.id === ride.id) {
      this.selectedRide.set({ ...ride, favorite: !ride.favorite });
    }
  }

  private initDetailMap(ride: Ride): void {
    if (this.detailMap) {
      this.detailMap.remove();
      this.detailMap = null;
    }
    this.mapMarkers = [];
    this.mapRouteLines = [];

    const mapElement = document.getElementById('detailMap');
    if (!mapElement) return;

    this.resetLeafletContainer('detailMap');

    void mapElement.offsetHeight;

    const startLat = Number(ride.startLocation.lat);
    const startLng = Number(ride.startLocation.lng);

    this.detailMap = L.map('detailMap').setView([startLat, startLng], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(this.detailMap);

    const startMarker = L.marker([startLat, startLng], {
      icon: this.coloredMarker('green'),
    })
      .addTo(this.detailMap)
      .bindPopup('Pickup: ' + (ride.startLocation.address ?? ''));
    this.mapMarkers.push(startMarker);

    (ride.stops ?? []).forEach((stop, index) => {
      const lat = Number(stop.lat);
      const lng = Number(stop.lng);

      const stopMarker = L.marker([lat, lng], {
        icon: this.coloredMarker('blue'),
      })
        .addTo(this.detailMap!)
        .bindPopup(`Stop ${index + 1}: ${stop.address ?? ''}`);
      this.mapMarkers.push(stopMarker);
    });

    const endLat = Number(ride.endLocation.lat);
    const endLng = Number(ride.endLocation.lng);

    const endMarker = L.marker([endLat, endLng], {
      icon: this.coloredMarker('red'),
    })
      .addTo(this.detailMap)
      .bindPopup('Destination: ' + (ride.endLocation.address ?? ''));
    this.mapMarkers.push(endMarker);

    this.drawRideRoute(ride);

    if (this.mapMarkers.length > 0) {
      const group = L.featureGroup(this.mapMarkers);
      this.detailMap.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRideRoute(ride: Ride): void {
    if (!this.detailMap) return;

    this.routeRequestId++;
    const requestId = this.routeRequestId;

    const waypoints = [
      ride.startLocation,
      ...(ride.stops ?? []),
      ride.endLocation,
    ];

    this.drawFullRoute(waypoints, requestId);
  }

  private async drawFullRoute(waypoints: Location[], requestId: number): Promise<void> {
    if (!this.detailMap) return;

    this.mapRouteLines.forEach(l => l.remove());
    this.mapRouteLines = [];

    const body = {
      coordinates: waypoints.map(wp => [Number(wp.lng), Number(wp.lat)]),
      instructions: false,
    };

    try {
      const res = await fetch(`${env.API_URL}/simple-routes/route?profile=driving-car`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/geo+json',
        },
        body: JSON.stringify(body),
      });

      if (requestId !== this.routeRequestId) return;

      const raw = await res.text();
      if (!res.ok) {
        console.error('Proxy route failed:', res.status, raw);
        return;
      }

      const data = JSON.parse(raw);
      const coords = data?.features?.[0]?.geometry?.coordinates;
      if (!coords?.length) return;

      const latLngs: [number, number][] = coords.map(([lon, lat]: [number, number]) => [lat, lon]);

      const routeLine = L.polyline(latLngs, { weight: 5, opacity: 0.9 })
        .addTo(this.detailMap);

      this.mapRouteLines.push(routeLine);

      this.detailMap.fitBounds(routeLine.getBounds().pad(0.1));

      setTimeout(() => this.detailMap?.invalidateSize(true), 0);
    } catch (err) {
      console.error('Error drawing full route:', err);
    }
  }

  private resetLeafletContainer(containerId: string): void {
    const el = document.getElementById(containerId) as any;
    if (el && el._leaflet_id) {
      el._leaflet_id = null;
    }
  }

  private coloredMarker(color: 'green' | 'blue' | 'red'): L.Icon {
    const urlMap: Record<string, string> = {
      green:
        'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
      blue:
        'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
      red:
        'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
    };

    return L.icon({
      iconUrl: urlMap[color],
      shadowUrl:
        'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
    });
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  protected formatDateTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected viewReport(): void {
    console.log('View report clicked');
  }

  protected bookAgain(event: Event, ride: Ride): void {
    event.stopPropagation();

    this.rideBookingService.clearRoute();

    this.rideBookingService.setPickupLocationDirect({
      name: ride.startLocation.address,
      lat: ride.startLocation.lat,
      lng: ride.startLocation.lng,
    })

    this.rideBookingService.setDestinationLocation({
      name: ride.endLocation.address,
      lat: ride.endLocation.lat,
      lng: ride.endLocation.lng,
    })

    this.rideBookingService.clearStops?.();
    if (ride.stops && ride.stops.length > 0) {
      ride.stops.forEach(stop => {
        this.rideBookingService.addStopLocation({
          name: stop.address,
          lat: stop.lat,
          lng: stop.lng,
        });
      });
    }

    this.rideBookingService.calculateRoute?.();
    this.router.navigate(['/']);
    this.selectedRide.set(null);
  }

  protected scheduleRide(event: Event, ride: Ride): void {
    event.stopPropagation();
    console.log('Schedule:', ride);
  }

  private routeText(r: RideForRouteText): string {
    return `${r.startLocation.address} -> ${r.endLocation.address}`;
  }

  private endMillis(r: RideForEndMillis): number {
    return r.endTime ? new Date(r.endTime).getTime() : 0;
  }

  private mapSummaryToRide(r: PassengerRideSummary): Ride {
    return {
      id: r.id,
      status: r.status,
      startLocation: r.startLocation,
      endLocation: r.endLocation,
      startTime: r.startTime,
      endTime: r.endTime ?? null,
      favorite: r.favorite,

      stops: [],
      driverName: '-',
      driverReview: null,
      rideReview: null,
      inconsistencyReport: [],
    };
  }

  private mergeDetails(base: Ride, details: PassengerRideDetails): Ride {
    const inconsistencyReport =
      (details.inconsistencyReports ?? []).map(rep => rep.reportText ?? 'Inconsistency report');

    return {
      ...base,
      status: details.status,
      startLocation: details.startLocation,
      endLocation: details.endLocation,
      startTime: details.startTime,
      endTime: details.endTime ?? null,
      favorite: details.favorite,

      stops: details.stops ?? [],
      driverName: details.driverName ?? '-',
      driverReview: details.driverReview ?? null,
      rideReview: details.rideReview ?? null,
      inconsistencyReport,
    };
  }

  private readonly REVIEW_WINDOW_DAYS = 3;

  protected canReview(ride: Ride): boolean {
    if (!ride.endTime) return false;

    const end = new Date(ride.endTime).getTime();
    const now = Date.now();
    const diffMs = now - end;

    if (diffMs < 0) return false;

    const threeDaysMs = this.REVIEW_WINDOW_DAYS * 24 * 60 * 60 * 1000;
    return diffMs <= threeDaysMs;
  }

  protected openReview(ride: Ride): void {
    this.router.navigate(['/ride-review', ride.id]);

  }

}
