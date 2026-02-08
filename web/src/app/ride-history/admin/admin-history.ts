import { Component, signal, computed, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { env } from '../../../env/env';
import { Router } from '@angular/router';
import { AdminHistoryService, AdminRideSummary, AdminRideDetails, Location } from '../../services/admin-history.service';
import { RatingModule} from 'primeng/rating';


type SortOption =
  | 'startTime-desc'
  | 'startTime-asc'
  | 'endTime-desc'
  | 'endTime-asc'
  | 'route-asc'
  | 'route-desc'
  | 'price-asc'
  | 'price-desc'
  | 'status-asc'
  | 'status-desc'
  | 'panic-asc'
  | 'panic-desc';

type RideForRouteText = {
  startLocation: { address: string };
  endLocation: { address: string };
};

type RideForEndMillis = {
  endTime?: string | null;
};

@Component({
  selector: 'app-admin-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RatingModule],
  templateUrl: './admin-history.html',
  styleUrls: ['./admin-history.css'],
})
export class AdminRideHistoryComponent implements OnInit, AfterViewInit {
  protected rides = signal<AdminRideSummary[]>([]);
  protected selectedRide = signal<AdminRideDetails | null>(null);

  userId: number | null = null;

  startDate: string = '';
  endDate: string = '';

  sortOption: SortOption = 'startTime-desc';

  private detailMap: L.Map | null = null;
  private mapMarkers: L.Marker[] = [];
  private mapRouteLines: L.Polyline[] = [];
  private routeRequestId = 0;

  constructor(
    private adminHistoryService: AdminHistoryService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRides();
  }

  ngAfterViewInit(): void {}

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

      case 'price-asc':
        sorted.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case 'price-desc':
        sorted.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;

      case 'status-asc':
        sorted.sort((a, b) => (a.status ?? '').localeCompare(b.status ?? ''));
        break;
      case 'status-desc':
        sorted.sort((a, b) => (b.status ?? '').localeCompare(a.status ?? ''));
        break;

      case 'panic-asc':
        sorted.sort((a, b) => Number(Boolean(a.panic)) - Number(Boolean(b.panic)));
        break;
      case 'panic-desc':
        sorted.sort((a, b) => Number(Boolean(b.panic)) - Number(Boolean(a.panic)));
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

  protected loadRides(): void {
    const from = this.startDate || undefined;
    const to = this.endDate || undefined;

    this.adminHistoryService.getAdminRides(from, to).subscribe({
      next: (rows: AdminRideSummary[]) => {
        const mapped = (rows ?? []).map(r => this.normalizeSummary(r));
        this.rides.set(mapped);
      },
      error: (err: any) => {
        console.error('getAdminRides failed:', err);
        this.rides.set([]);
      },
    });
  }

  protected openRideDetails(ride: AdminRideSummary): void {
    this.adminHistoryService.getAdminRideDetails(ride.id).subscribe({
      next: (details: AdminRideDetails) => {
        this.selectedRide.set(this.mergeDetails(ride, details));
        setTimeout(() => this.initDetailMap(this.selectedRide()!), 300);
      },
      error: (err: any) => {
        console.error('getAdminRideDetails failed:', err);
        const fallback: AdminRideDetails = {
          ...ride,

          stops: [],
          driverName: '-',
          passengerEmails: [],
          driverReview: null,
          rideReview: null,
          inconsistencyReports: [],
          cancellationReason: null,
        };

        this.selectedRide.set(fallback);
        setTimeout(() => this.initDetailMap(fallback), 300);
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

  private initDetailMap(ride: AdminRideSummary | AdminRideDetails): void {
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

    const stops = 'stops' in ride ? (ride.stops ?? []) : [];

    stops.forEach((stop, index) => {
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

    this.drawRideRoute(ride, stops);

    if (this.mapMarkers.length > 0) {
      const group = L.featureGroup(this.mapMarkers);
      this.detailMap.fitBounds(group.getBounds().pad(0.1));
    }
  }

  private drawRideRoute(ride: AdminRideSummary, stops: Location[]): void {
    if (!this.detailMap) return;

    this.routeRequestId++;
    const requestId = this.routeRequestId;

    const waypoints: Location[] = [ride.startLocation, ...(stops ?? []), ride.endLocation];

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
          Accept: 'application/geo+json',
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

      const routeLine = L.polyline(latLngs, { weight: 5, opacity: 0.9 }).addTo(this.detailMap);
      this.mapRouteLines.push(routeLine);

      this.detailMap.fitBounds(routeLine.getBounds().pad(0.1));
      setTimeout(() => this.detailMap?.invalidateSize(true), 0);
    } catch (err) {
      console.error('Error drawing full route:', err);
    }
  }

  private resetLeafletContainer(containerId: string): void {
    const el = document.getElementById(containerId) as any;
    if (el && el._leaflet_id) el._leaflet_id = null;
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
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
    });
  }

  protected getStatusText(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'Completed';
      case 'CANCELLED_BY_DRIVER':
        return 'Cancelled by Driver';
      case 'CANCELLED_BY_PASSENGER':
        return 'Cancelled by Passenger';
      case 'FINISHED_EARLY':
        return 'Finished Early';
      default:
        return status;
    }
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  protected formatDateTime(dateString: string | null): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected formatDateTimeShort(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected viewReport(): void {
    console.log('Admin: View report clicked');
  }

  private mapLoc(loc: any): Location {
    return {
      address: String(loc?.address ?? ''),
      lat: Number(loc?.lat ?? 0),
      lng: Number(loc?.lng ?? 0),
    };
  }

  private normalizeSummary(r: any): AdminRideSummary {
    const panic = Boolean(r?.panic);

    return {
      id: Number(r?.id ?? 0),
      status: r?.status,

      startLocation: this.mapLoc(r?.startLocation),
      endLocation: this.mapLoc(r?.endLocation),

      startTime: r?.startTime,
      endTime: r?.endTime ?? null,

      favorite: Boolean(r?.favorite),
      price: Number(r?.price ?? 0),
      panic,

    };
  }

  private mergeDetails(base: AdminRideSummary, details: AdminRideDetails): AdminRideDetails {
    return {
      ...base,

      status: details.status ?? base.status,
      startLocation: details.startLocation ?? base.startLocation,
      endLocation: details.endLocation ?? base.endLocation,
      startTime: details.startTime ?? base.startTime,
      endTime: details.endTime ?? base.endTime,
      price: Number((details as any)?.price ?? base.price),
      panic: Boolean((details as any)?.panic ?? base.panic),

      stops: (details.stops ?? []).map(s => this.mapLoc(s)),

      driverName: (details as any)?.driverName ?? '-',
      passengerEmails: (details as any)?.passengerEmails ?? [],

      driverReview: (details as any)?.driverReview ?? null,
      rideReview: (details as any)?.rideReview ?? null,

      inconsistencyReports: (details as any)?.inconsistencyReports ?? [],

      cancellationReason: (details as any)?.cancellationReason ?? null,
    };
  }

  private routeText(r: RideForRouteText): string {
    return `${r.startLocation.address} -> ${r.endLocation.address}`;
  }

  private endMillis(r: RideForEndMillis): number {
    return r.endTime ? new Date(r.endTime).getTime() : 0;
  }

  protected resetFilters(): void {
    this.startDate = '';
    this.endDate = '';
    this.sortOption = 'startTime-desc';

    this.rides.set([...this.rides()]);
  }
}
