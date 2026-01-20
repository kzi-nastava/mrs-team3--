import { Component, signal, computed, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DriverHistoryService, DriverRide } from '../../services/driver-history.service';
import { AuthService } from '../../services/auth.service';
import * as L from 'leaflet';
import { env } from '../../../env/env';

@Component({
  selector: 'app-driver-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-history.html',
  styleUrls: ['./driver-history.css'],
})
export class DriverHistoryComponent implements OnInit, AfterViewInit {

  protected rides = signal<DriverRide[]>([]);
  protected selectedRide = signal<DriverRide | null>(null);
  
  // Filter states
  startDate: string = '';
  endDate: string = '';

  // Map
  private detailMap: any = null;
  private mapMarkers: any[] = [];
  private mapRouteLines: any[] = [];
  private routeRequestId = 0;

  constructor(
    private driverHistoryService: DriverHistoryService,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadRides();
  }

  ngAfterViewInit(): void {
    // Map will be initialized when modal opens
  }

  private loadRides(): void {
    const driverId = this.authService.getUserId();
    
    if (!driverId) {
      console.error('No driver ID found');
      this.rides.set([]);
      return;
    }
    
    this.driverHistoryService.getRides(driverId).subscribe({
      next: (rides) => {
        this.rides.set(rides);
      },
      error: (err) => {
        console.error('Error loading rides:', err);
        this.rides.set([]);
      }
    });
  }

  protected filteredRides = computed(() => {
    let rides = this.rides();

    if (this.startDate) {
      rides = rides.filter(r => r.startTime >= this.startDate);
    }

    if (this.endDate) {
      const endDateTime = new Date(this.endDate);
      endDateTime.setHours(23, 59, 59, 999);
      rides = rides.filter(r => new Date(r.startTime) <= endDateTime);
    }

    return rides.sort((a, b) => 
      new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
    );
  });

  protected onFilterChange(): void {
    const driverId = this.authService.getUserId();
    
    if (!driverId) {
      console.error('No driver ID found');
      return;
    }
    
    this.driverHistoryService.getRides(
      driverId,
      this.startDate || undefined,
      this.endDate || undefined
    ).subscribe({
      next: (rides) => {
        this.rides.set(rides);
      },
      error: (err) => {
        console.error('Error filtering rides:', err);
        this.rides.set([]);
      }
    });
  }

  protected openRideDetails(ride: DriverRide): void {
    const driverId = this.authService.getUserId();
    
    if (!driverId) {
      console.error('No driver ID found');
      return;
    }
    
    this.driverHistoryService.getRideDetail(driverId, ride.id).subscribe({
      next: (detailedRide) => {
        this.selectedRide.set(detailedRide);
        
        setTimeout(() => {
          this.initDetailMap(detailedRide);
        }, 100);
      },
      error: (err) => {
        console.error('Error loading ride details:', err);
        this.selectedRide.set(ride);
        setTimeout(() => {
          this.initDetailMap(ride);
        }, 100);
      }
    });
  }

  protected closeModal(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.selectedRide.set(null);
    
    if (this.detailMap) {
      this.detailMap.remove();
      this.detailMap = null;
      this.mapMarkers = [];
      this.mapRouteLines = [];
    }
  }

  private initDetailMap(ride: DriverRide): void {
    // console.log('Initializing map with ride:', ride);
    // console.log('Start location:', ride.startLocation);
    // console.log('Stops:', ride.stops);
    // console.log('End location:', ride.endLocation);
    
    if (this.detailMap) {
      this.detailMap.remove();
    }

    const mapElement = document.getElementById('detailMap');
    if (!mapElement) {
      console.error('Map element not found!');
      return;
    }
    

    this.detailMap = L.map('detailMap').setView(
      [ride.startLocation.latitude, ride.startLocation.longitude],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.detailMap);

    // Add start marker
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

    // Add stop markers
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

    // Add end marker
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

    this.drawRideRoute(ride);
  }

  private drawRideRoute(ride: DriverRide): void {
    this.routeRequestId++;
    const requestId = this.routeRequestId;

    const waypoints = [
      ride.startLocation,
      ...ride.stops,
      ride.endLocation
    ];

    for (let i = 0; i < waypoints.length - 1; i++) {
      this.drawRouteBetween(waypoints[i], waypoints[i + 1], requestId);
    }

    if (this.mapMarkers.length > 0) {
      const group = L.featureGroup(this.mapMarkers);
      this.detailMap.fitBounds(group.getBounds().pad(0.1));
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
        }).addTo(this.detailMap);

        this.mapRouteLines.push(routeLine);
      })
      .catch(err => {
        console.error('Error drawing route:', err);
      });
  }

  protected viewReport(): void {
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
      case 'FINISHED_EARLY':
        return 'Finished Early';
      default:
        return status;
    }
  }

  protected formatDate(dateString: string | null): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  protected formatDateTime(dateString: string | null): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  protected getStarArray(rating: number): boolean[] {
    return [1, 2, 3, 4, 5].map(star => star <= rating);
  }
}