import { Component, signal, computed, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { env } from '../../../env/env';
import { Router } from '@angular/router';
import {
  AdminHistoryService,
  AdminRideSummary,
  AdminRideDetails,
  Location
} from '../../services/admin-history.service';
import { RatingModule } from 'primeng/rating';

type SortField = 'startTime' | 'endTime' | 'route' | 'price' | 'status' | 'panic';
type SortDir = 'asc' | 'desc';

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

  // Filters (ostavljeno kao string, minimal diff)
  startDate: string = '';
  endDate: string = '';

  // ✅ NEW: jedan select key (npr. "price|asc")
  sortKey: string = 'startTime|desc';

  // Interno parsirano (field + dir)
  private sortField: SortField = 'startTime';
  private sortDir: SortDir = 'desc';

  private detailMap: L.Map | null = null;
  private mapMarkers: L.Marker[] = [];
  private mapRouteLines: L.Polyline[] = [];
  private routeRequestId = 0;

  constructor(
    private adminHistoryService: AdminHistoryService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // osiguraj da su sortField/sortDir inicijalno usklađeni sa sortKey
    this.applySortKey(this.sortKey);
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

    const dirMul = this.sortDir === 'asc' ? 1 : -1;
    const field = this.sortField;

    sorted.sort((a, b) => {
      switch (field) {
        case 'startTime':
          return dirMul * (new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

        case 'endTime':
          return dirMul * (this.endMillis(a) - this.endMillis(b));

        case 'route':
          return dirMul * this.routeText(a).localeCompare(this.routeText(b));

        case 'price':
          return dirMul * ((a.price ?? 0) - (b.price ?? 0));

        case 'status':
          return dirMul * ((a.status ?? '').localeCompare(b.status ?? ''));

        case 'panic':
          return dirMul * (Number(Boolean(a.panic)) - Number(Boolean(b.panic)));
      }
    });

    return sorted;
  });

  // ✅ Pozovi ovo iz HTML-a na (ngModelChange)
  protected onSortKeyChange(value: string): void {
    this.sortKey = value;
    this.applySortKey(value);

    // Minimal diff: zadržavamo postojeći "refresh" pattern
    this.rides.set([...this.rides()]);
  }

  protected onFilterChange(): void {
    // Minimal diff: zadržavamo postojeći "refresh" pattern
    this.rides.set([...this.rides()]);
  }

  private applySortKey(value: string): void {
    const [fieldRaw, dirRaw] = (value || '').split('|');

    const field = (fieldRaw as SortField) || 'startTime';
    const dir = (dirRaw as SortDir) || 'desc';

    // Bezbedni fallback (ako value nije validan)
    const allowedFields: SortField[] = ['startTime', 'endTime', 'route', 'price', 'status', 'panic'];
    const allowedDirs: SortDir[] = ['asc', 'desc'];

    this.sortField = allowedFields.includes(field) ? field : 'startTime';
    this.sortDir = allowedDirs.includes(dir) ? dir : 'desc';
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

    this.sortKey = 'startTime|desc';
    this.applySortKey(this.sortKey);

    this.rides.set([...this.rides()]);
  }

  // --- Date Picker state ---
  protected datePickerOpen = false;
  private datePickerTarget: 'from' | 'to' = 'from';

  // first day of the currently displayed month (local time)
  private dpView = new Date(new Date().getFullYear(), new Date().getMonth(), 1);

  protected dpMonthLabel = '';
  protected dpCells: Array<null | { day: number; iso: string }> = [];

  private buildDatePicker(): void {
    const y = this.dpView.getFullYear();
    const m = this.dpView.getMonth(); // 0-based

    const monthNames = [
      'January','February','March','April','May','June',
      'July','August','September','October','November','December',
    ];
    this.dpMonthLabel = `${monthNames[m]} ${y}`;

    const firstOfMonth = new Date(y, m, 1);
    const daysInMonth = new Date(y, m + 1, 0).getDate();

    // Make Monday=0 ... Sunday=6
    const jsDay = firstOfMonth.getDay(); // Sun=0 ... Sat=6
    const mondayIndex = (jsDay + 6) % 7;

    const cells: Array<null | { day: number; iso: string }> = [];

    // leading blanks
    for (let i = 0; i < mondayIndex; i++) cells.push(null);

    // actual days
    for (let d = 1; d <= daysInMonth; d++) {
      const iso = this.toISODate(new Date(y, m, d));
      cells.push({ day: d, iso });
    }

    // trailing blanks to complete the last week row
    while (cells.length % 7 !== 0) cells.push(null);

    this.dpCells = cells;
  }

  private toISODate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  protected openDatePicker(target: 'from' | 'to', ev?: Event): void {
    ev?.stopPropagation?.();
    this.datePickerTarget = target;

    // If target already has a date, open the calendar on that month
    const value = target === 'from' ? this.startDate : this.endDate;
    if (value) {
      const [yy, mm] = value.split('-').map(Number);
      if (yy && mm) this.dpView = new Date(yy, mm - 1, 1);
    }

    this.buildDatePicker();
    this.datePickerOpen = true;
  }

  protected closeDatePicker(): void {
    this.datePickerOpen = false;
  }

  protected dpPrevMonth(): void {
    this.dpView = new Date(this.dpView.getFullYear(), this.dpView.getMonth() - 1, 1);
    this.buildDatePicker();
  }

  protected dpNextMonth(): void {
    this.dpView = new Date(this.dpView.getFullYear(), this.dpView.getMonth() + 1, 1);
    this.buildDatePicker();
  }

  protected selectDate(iso: string): void {
    if (this.datePickerTarget === 'from') this.startDate = iso;
    else this.endDate = iso;

    this.onFilterChange();
    this.closeDatePicker();
  }

  protected clearDate(): void {
    if (this.datePickerTarget === 'from') this.startDate = '';
    else this.endDate = '';

    this.onFilterChange();
    this.closeDatePicker();
  }

  protected pickToday(): void {
    const iso = this.toISODate(new Date());
    this.selectDate(iso);
  }
}
