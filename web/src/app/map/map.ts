import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import * as L from 'leaflet';
import { RideService, Location as RideLocation } from '../services/ride.service';
import { RideBookingService, Location as BookingLocation } from '../services/ride-booking.service';
import { Subject, takeUntil } from 'rxjs';
import { env } from '../../env/env';

type Location = RideLocation | BookingLocation;

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [],
  templateUrl: './map.html',
  styleUrl: './map.css'
})
export class MapComponent implements OnInit, OnDestroy {
  @Input() mode: 'simple' | 'booking' = 'simple';

  private destroy$ = new Subject<void>();
  private map: any;

  /** ✅ DODATO – invalidacija asinhronih ruta */
  private routeRequestId = 0;

  // Simple mode
  private startMarker: any = null;
  private endMarker: any = null;
  private routeLine: any = null;

  // Booking mode
  private pickupMarker: any = null;
  private stopMarkers: any[] = [];
  private destinationMarker: any = null;
  private routeLines: any[] = [];

  constructor(
    private rideService: RideService,
    private rideBookingService: RideBookingService
  ) {}

  ngOnInit(): void {
    this.initMap();

    if (this.mode === 'simple') {
      this.subscribeToSimpleMode();
    } else {
      this.subscribeToBookingMode();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initMap(): void {
    this.map = L.map('map').setView([45.2671, 19.8335], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      this.addLocationMarker(e.latlng);
    });

    if (this.mode === 'booking') {
      this.map.on('contextmenu', (e: any) => {
        this.addDestinationFromMap(e.latlng);
      });
    }
  }

  // ================= SIMPLE MODE =================

  private subscribeToSimpleMode(): void {
    this.rideService.rideData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.start) this.addStartMarker(data.start);
        if (data.end) this.addEndMarker(data.end);
        if (data.start && data.end) this.drawSimpleRoute();
      });

    this.rideService.clearRoute$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.clearSimpleMarkers();
      });

    this.rideService.calculateRoute$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.drawSimpleRoute();
      });
  }

  private addStartMarker(location: Location): void {
    if (this.startMarker) this.map.removeLayer(this.startMarker);

    const latlng = { lat: location.lat, lng: location.lng };

    this.startMarker = L.marker(latlng, {
      icon: L.icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })
    }).addTo(this.map).bindPopup('Start').openPopup();

    this.map.setView(latlng, 14);
  }

  private addEndMarker(location: Location): void {
    if (this.endMarker) this.map.removeLayer(this.endMarker);

    const latlng = { lat: location.lat, lng: location.lng };

    this.endMarker = L.marker(latlng, {
      icon: L.icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })
    }).addTo(this.map).bindPopup('End').openPopup();

    this.map.setView(latlng, 14);
  }

  private drawSimpleRoute(): void {
    if (!this.startMarker || !this.endMarker) return;

    /** ✅ DODATO */
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    const startLatLng = this.startMarker.getLatLng();
    const endLatLng = this.endMarker.getLatLng();

    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${startLatLng.lng},${startLatLng.lat}&end=${endLatLng.lng},${endLatLng.lat}`;

    fetch(url)
      .then(r => r.json())
      .then(data => {
        /** ✅ DODATO */
        if (requestId !== this.routeRequestId) return;

        const coordinates = data.features[0].geometry.coordinates;
        const routeCoordinates = coordinates.map((c: any) => [c[1], c[0]]);

        if (this.routeLine) this.map.removeLayer(this.routeLine);

        this.routeLine = L.polyline(routeCoordinates, {
          color: 'blue',
          weight: 4
        }).addTo(this.map);

        this.map.fitBounds(this.routeLine.getBounds(), { padding: [50, 50] });
      });
  }

  private clearSimpleMarkers(): void {
    /** ✅ DODATO */
    this.routeRequestId++;

    if (this.startMarker) this.map.removeLayer(this.startMarker);
    if (this.endMarker) this.map.removeLayer(this.endMarker);
    if (this.routeLine) this.map.removeLayer(this.routeLine);

    this.startMarker = null;
    this.endMarker = null;
    this.routeLine = null;
  }

  // ================= BOOKING MODE =================

  private subscribeToBookingMode(): void {
    this.rideBookingService.rideBookingData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.pickup) this.addPickupMarker(data.pickup);
        this.updateStopMarkers(data.stops);
        if (data.destination) this.addDestinationMarker(data.destination);
        if (data.pickup && data.destination) this.drawBookingRoutes();
      });

    this.rideBookingService.clearRoute$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.clearBookingMarkers();
      });

    this.rideBookingService.calculateRoute$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.drawBookingRoutes();
      });
  }

  private addPickupMarker(location: Location): void {
    if (this.pickupMarker) this.map.removeLayer(this.pickupMarker);

    const latlng = { lat: location.lat, lng: location.lng };

    this.pickupMarker = L.marker(latlng, {
      icon: L.icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })
    }).addTo(this.map).bindPopup('Pickup').openPopup();
  }

  private updateStopMarkers(stops: Location[]): void {
    this.stopMarkers.forEach(m => this.map.removeLayer(m));
    this.stopMarkers = [];

    stops.forEach((stop, index) => {
      const latlng = { lat: stop.lat, lng: stop.lng };

      const marker = L.marker(latlng, {
        icon: L.icon({
          iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
          shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41]
        })
      }).addTo(this.map).bindPopup(`Stop ${index + 1}`);

      this.stopMarkers.push(marker);
    });
  }

  private addDestinationMarker(location: Location): void {
    if (this.destinationMarker) this.map.removeLayer(this.destinationMarker);

    const latlng = { lat: location.lat, lng: location.lng };

    this.destinationMarker = L.marker(latlng, {
      icon: L.icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })
    }).addTo(this.map).bindPopup('Destination').openPopup();
  }

  private drawBookingRoutes(): void {
    /** ✅ DODATO */
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    this.routeLines.forEach(l => this.map.removeLayer(l));
    this.routeLines = [];

    const rideData = this.rideBookingService.getRideBookingData();
    if (!rideData.pickup || !rideData.destination) return;

    const waypoints: Location[] = [
      rideData.pickup,
      ...rideData.stops,
      rideData.destination
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1], i, requestId);
    }
  }

  private drawRouteBetween(
    start: Location,
    end: Location,
    segmentIndex: number,
    requestId: number
  ): void {
    const apiKey = env.MAPS_KEY;
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.lng},${start.lat}&end=${end.lng},${end.lat}`;

    fetch(url)
      .then(r => r.json())
      .then(data => {
        /** ✅ DODATO */
        if (requestId !== this.routeRequestId) return;

        const coords = data.features[0].geometry.coordinates;
        const routeCoords = coords.map((c: any) => [c[1], c[0]]);

        const routeLine = L.polyline(routeCoords, {
          color: 'blue',
          weight: 4,
          opacity: 0.7
        }).addTo(this.map);

        this.routeLines.push(routeLine);
      });
  }

  private clearBookingMarkers(): void {
    /** ✅ DODATO */
    this.routeRequestId++;

    if (this.pickupMarker) this.map.removeLayer(this.pickupMarker);
    if (this.destinationMarker) this.map.removeLayer(this.destinationMarker);

    this.stopMarkers.forEach(m => this.map.removeLayer(m));
    this.routeLines.forEach(l => this.map.removeLayer(l));

    this.pickupMarker = null;
    this.destinationMarker = null;
    this.stopMarkers = [];
    this.routeLines = [];
  }

  // ================= MAP INTERACTION =================

  private addDestinationFromMap(latlng: any): void {
    const location: Location = {
      lat: latlng.lat,
      lng: latlng.lng,
      name: `${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)}`
    };

    this.rideBookingService.setDestinationLocation(location);
  }

  private addLocationMarker(latlng: any): void {
    const location: Location = {
      lat: latlng.lat,
      lng: latlng.lng,
      name: `${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)}`
    };

    if (this.mode === 'simple') {
      const rideData = this.rideService.getRideData();

      if (!rideData.start) this.rideService.setStartLocation(location);
      else if (!rideData.end) this.rideService.setEndLocation(location);
      else this.rideService.clearRoute();
    } else {
      const rideData = this.rideBookingService.getRideBookingData();

      if (!rideData.pickup) this.rideBookingService.setPickupLocation(location);
      else this.rideBookingService.addStopLocation(location);
    }
  }
}
