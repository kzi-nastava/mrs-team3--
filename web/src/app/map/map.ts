import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import * as L from 'leaflet';
import { RideService, Location as RideLocation } from '../services/ride.service';
import { RideBookingService, Location as BookingLocation } from '../services/ride-booking.service';
import { Subject, takeUntil } from 'rxjs';

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

  // Left click
  this.map.on('click', (e: any) => {
    this.addLocationMarker(e.latlng);
  });

  // Right click - for booking mode only
  if (this.mode === 'booking') {
    this.map.on('contextmenu', (e: any) => {
      this.addDestinationFromMap(e.latlng);
    });
  }
}

  // For non-registered users - simple mode

  private subscribeToSimpleMode(): void {
    this.rideService.rideData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.start) {
          this.addStartMarker(data.start);
        }
        if (data.end) {
          this.addEndMarker(data.end);
        }
        if (data.start && data.end) {
          this.drawSimpleRoute();
        }
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
    if (this.startMarker) {
      this.map.removeLayer(this.startMarker);
    }

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
    if (this.endMarker) {
      this.map.removeLayer(this.endMarker);
    }

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
    if (this.startMarker && this.endMarker) {
      const startLatLng = this.startMarker.getLatLng();
      const endLatLng = this.endMarker.getLatLng();

      const apiKey = 'add you own openrouteservice api key here'; //Add you key here
      const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${startLatLng.lng},${startLatLng.lat}&end=${endLatLng.lng},${endLatLng.lat}`;

      fetch(url)
        .then(response => response.json())
        .then(data => {
          const coordinates = data.features[0].geometry.coordinates;
          const routeCoordinates = coordinates.map((coord: any) => [coord[1], coord[0]]);

          if (this.routeLine) {
            this.map.removeLayer(this.routeLine);
          }

          this.routeLine = L.polyline(routeCoordinates, {
            color: 'blue',
            weight: 4
          }).addTo(this.map);

          this.map.fitBounds(this.routeLine.getBounds(), { padding: [50, 50] });
        })
        .catch(error => {
          console.error('Routing error:', error);
          alert('Failed to calculate route. Please try again.');
        });
    }
  }

  private clearSimpleMarkers(): void {
    if (this.startMarker) {
      this.map.removeLayer(this.startMarker);
      this.startMarker = null;
    }
    if (this.endMarker) {
      this.map.removeLayer(this.endMarker);
      this.endMarker = null;
    }
    if (this.routeLine) {
      this.map.removeLayer(this.routeLine);
      this.routeLine = null;
    }
  }

  // For registered users - booking mode

  private subscribeToBookingMode(): void {
    this.rideBookingService.rideBookingData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (data.pickup) {
          this.addPickupMarker(data.pickup);
        }
        this.updateStopMarkers(data.stops);
        if (data.destination) {
          this.addDestinationMarker(data.destination);
        }
        if (data.pickup && data.destination) {
          this.drawBookingRoutes();
        }
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
    if (this.pickupMarker) {
      this.map.removeLayer(this.pickupMarker);
    }

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

    this.map.setView(latlng, 14);
  }

  private updateStopMarkers(stops: Location[]): void {
    this.stopMarkers.forEach(marker => {
      this.map.removeLayer(marker);
    });
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
    if (this.destinationMarker) {
      this.map.removeLayer(this.destinationMarker);
    }

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

    this.map.setView(latlng, 14);
  }

  private drawBookingRoutes(): void {
    this.routeLines.forEach(line => {
      this.map.removeLayer(line);
    });
    this.routeLines = [];

    const rideData = this.rideBookingService.getRideBookingData();

    if (!rideData.pickup || !rideData.destination) {
      return;
    }

    const waypoints: Location[] = [
      rideData.pickup,
      ...rideData.stops,
      rideData.destination
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1], i);
    }
  }

  private drawRouteBetween(start: Location, end: Location, segmentIndex: number): void {
    const apiKey = 'Add also here=';
    const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.lng},${start.lat}&end=${end.lng},${end.lat}`;

    fetch(url)
      .then(response => response.json())
      .then(data => {
        const coordinates = data.features[0].geometry.coordinates;
        const routeCoordinates = coordinates.map((coord: any) => [coord[1], coord[0]]);

        const routeLine = L.polyline(routeCoordinates, {
          color: 'blue',
          weight: 4,
          opacity: 0.7
        }).addTo(this.map);

        this.routeLines.push(routeLine);

        if (segmentIndex === 0 && this.routeLines.length === 1) {
          const bounds = L.latLngBounds([]);

          if (this.pickupMarker) bounds.extend(this.pickupMarker.getLatLng());
          this.stopMarkers.forEach(marker => bounds.extend(marker.getLatLng()));
          if (this.destinationMarker) bounds.extend(this.destinationMarker.getLatLng());

          this.map.fitBounds(bounds, { padding: [50, 50] });
        }
      })
      .catch(error => {
        console.error('Routing error:', error);
      });
  }

  private clearBookingMarkers(): void {
    if (this.pickupMarker) {
      this.map.removeLayer(this.pickupMarker);
      this.pickupMarker = null;
    }

    this.stopMarkers.forEach(marker => {
      this.map.removeLayer(marker);
    });
    this.stopMarkers = [];

    if (this.destinationMarker) {
      this.map.removeLayer(this.destinationMarker);
      this.destinationMarker = null;
    }

    this.routeLines.forEach(line => {
      this.map.removeLayer(line);
    });
    this.routeLines = [];
  }

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

    if (!rideData.start) {
      this.rideService.setStartLocation(location);
    } else if (!rideData.end) {
      this.rideService.setEndLocation(location);
    } else {
      this.rideService.clearRoute();
      setTimeout(() => {
        this.addLocationMarker(latlng);
      }, 100);
    }
  } else {
    // Left click adds pickup or stop
    const rideData = this.rideBookingService.getRideBookingData();

    // Add stop or pickup
    if (!rideData.pickup) {
      this.rideBookingService.setPickupLocation(location);
    } else {
      this.rideBookingService.addStopLocation(location);
    }
  }
}
}
