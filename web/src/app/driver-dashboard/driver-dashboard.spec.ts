// import { Component, OnInit, OnDestroy, signal } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { DriverRideService, DriverRide, PendingRide, Location } from '../services/driver-ride.service';
// import * as L from 'leaflet';
// import { env } from '../../env/env';

// @Component({
//   selector: 'app-driver-dashboard',
//   standalone: true,
//   imports: [CommonModule],
//   templateUrl: './driver-dashboard.html',
//   styleUrls: ['./driver-dashboard.css']
// })
// export class DriverDashboardComponent implements OnInit, OnDestroy {
//   protected myRides = signal<DriverRide[]>([]);
//   protected pendingRides = signal<PendingRide[]>([]);
//   protected selectedRide = signal<DriverRide | PendingRide | null>(null);
//   protected loading = signal<boolean>(true);

//   // Map
//   private map: any = null;
//   private markers: any[] = [];
//   private routeLines: any[] = [];
//   private driverMarker: any = null;

//   // Modals
//   protected showFinishModal = signal<boolean>(false);

//   constructor(private service: DriverRideService) {}

//   ngOnInit(): void {
//     this.loadData();
//   }

//   ngOnDestroy(): void {
//     if (this.map) this.map.remove();
//   }

//   private loadData(): void {
//     this.loading.set(true);
    
//     this.service.getMyRides().subscribe({
//       next: (rides) => {
//         this.myRides.set(rides);
//         this.loading.set(false);

//         if (rides.length > 0) {
//           this.selectRide(rides[0]);
//         } else {
//           this.loadPendingRides();
//         }
//       },
//       error: () => {
//         this.loading.set(false);
//         this.loadPendingRides();
//       }
//     });
//   }

//   private loadPendingRides(): void {
//     this.service.getPendingRides().subscribe({
//       next: (rides) => {
//         this.pendingRides.set(rides);
//       }
//     });
//   }

//   protected selectRide(ride: DriverRide | PendingRide): void {
//     this.selectedRide.set(ride);
//     setTimeout(() => this.initMap(), 100);
//   }

//   protected acceptRide(ride: PendingRide): void {
//     this.service.acceptRide(ride.rideId).subscribe({
//       next: () => this.loadData(),
//       error: (err) => alert('Failed to accept: ' + err.message)
//     });
//   }

//   protected moveToStart(): void {
//     this.service.moveToStart().subscribe({
//       next: () => {
//         const ride = this.selectedRide() as DriverRide;
//         this.updateDriverMarker(ride.startLocation);
//       }
//     });
//   }

//   protected startRide(): void {
//     const ride = this.selectedRide() as DriverRide;
//     this.service.startRide(ride.rideId).subscribe({
//       next: () => this.loadData()
//     });
//   }

//   protected openFinishModal(): void {
//     this.showFinishModal.set(true);
//   }

//   protected closeFinishModal(): void {
//     this.showFinishModal.set(false);
//   }

//   protected finishRide(): void {
//     this.service.finishRide(null).subscribe({
//       next: (response) => {
//         this.closeFinishModal();
//         alert(`Ride completed! ${response.hasNextRide ? 'Next ride is active.' : 'You are available.'}`);
//         this.loadData();
//       }
//     });
//   }

//   protected markStopReached(stopIndex: number): void {
//     this.service.reachStop(stopIndex).subscribe({
//       next: (updatedRide) => {
//         // Update selected ride with new stop statuses
//         this.selectedRide.set(updatedRide);
//         alert(`Stop ${stopIndex + 1} marked as reached!`);
//       }
//     });
//   }

//   // Map Methods

//   private initMap(): void {
//     const ride = this.selectedRide();
//     if (!ride) return;

//     const mapElement = document.getElementById('driverMap');
//     if (!mapElement) return;

//     if (this.map) this.map.remove();

//     this.map = L.map('driverMap').setView(
//       [ride.startLocation.lat, ride.startLocation.lng],
//       13
//     );

//     L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
//       maxZoom: 19,
//       attribution: '© OpenStreetMap'
//     }).addTo(this.map);

//     // For IN_PROGRESS rides, allow clicking to move
//     if (this.isDriverRide(ride) && ride.status === 'IN_PROGRESS') {
//       this.map.on('click', (e: any) => {
//         this.moveDriverTo(e.latlng);
//       });
//     }

//     this.addMarkers(ride);
//     this.drawRoute(ride);
//   }

//   private addMarkers(ride: DriverRide | PendingRide): void {
//     this.markers.forEach(m => this.map.removeLayer(m));
//     this.markers = [];

//     // Start marker (green)
//     const startMarker = L.marker([ride.startLocation.lat, ride.startLocation.lng], {
//       icon: this.getIcon('green')
//     }).addTo(this.map).bindPopup('Pickup: ' + ride.startLocation.address);
//     this.markers.push(startMarker);

//     // Stop markers (blue for unreached, gold for reached)
//     ride.stops.forEach((stop, index) => {
//       const reached = this.isDriverRide(ride) 
//         ? ride.stopStatuses[index]?.reached 
//         : false;
      
//       const color = reached ? 'gold' : 'blue';
//       const label = reached ? `Stop ${index + 1} ✓` : `Stop ${index + 1}`;
      
//       const stopMarker = L.marker([stop.lat, stop.lng], {
//         icon: this.getIcon(color)
//       }).addTo(this.map).bindPopup(label);
//       this.markers.push(stopMarker);
//     });

//     // End marker (red)
//     const endMarker = L.marker([ride.endLocation.lat, ride.endLocation.lng], {
//       icon: this.getIcon('red')
//     }).addTo(this.map).bindPopup('Destination: ' + ride.endLocation.address);
//     this.markers.push(endMarker);

//     // Driver marker for IN_PROGRESS
//     if (this.isDriverRide(ride) && ride.status === 'IN_PROGRESS') {
//       this.driverMarker = L.marker([ride.startLocation.lat, ride.startLocation.lng], {
//         icon: L.divIcon({
//           className: 'car-marker',
//           html: '<div style="font-size: 30px;">🚗</div>',
//           iconSize: [30, 30],
//           iconAnchor: [15, 15]
//         })
//       }).addTo(this.map).bindPopup('You are here');
//     }

//     if (this.markers.length > 0) {
//       const group = L.featureGroup(this.markers);
//       this.map.fitBounds(group.getBounds().pad(0.1));
//     }
//   }

//   private drawRoute(ride: DriverRide | PendingRide): void {
//     this.routeLines.forEach(l => this.map.removeLayer(l));
//     this.routeLines = [];

//     const waypoints = [ride.startLocation, ...ride.stops, ride.endLocation];

//     for (let i = 0; i < waypoints.length - 1; i++) {
//       this.drawRouteBetween(waypoints[i], waypoints[i + 1]);
//     }
//   }

//   private drawRouteBetween(start: Location, end: Location): void {
//     const apiKey = env.MAPS_KEY;
//     const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${start.lng},${start.lat}&end=${end.lng},${end.lat}`;

//     fetch(url)
//       .then(r => r.json())
//       .then(data => {
//         const coords = data.features[0].geometry.coordinates;
//         const routeCoords = coords.map((c: any) => [c[1], c[0]]);

//         const routeLine = L.polyline(routeCoords, {
//           color: '#6366f1',
//           weight: 4,
//           opacity: 0.7
//         }).addTo(this.map);

//         this.routeLines.push(routeLine);
//       });
//   }

//   private moveDriverTo(latlng: any): void {
//     if (!this.driverMarker) return;

//     this.driverMarker.setLatLng(latlng);

//     this.service.updateLocation(latlng.lat, latlng.lng).subscribe();
//   }

//   private updateDriverMarker(location: Location): void {
//     if (this.driverMarker) {
//       this.driverMarker.setLatLng([location.lat, location.lng]);
//       this.map.setView([location.lat, location.lng], 15);
//     }
//   }

//   private getIcon(color: string): any {
//     const colorMap: any = {
//       green: 'marker-icon-2x-green.png',
//       blue: 'marker-icon-2x-blue.png',
//       red: 'marker-icon-2x-red.png',
//       gold: 'marker-icon-2x-gold.png'
//     };

//     return L.icon({
//       iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/${colorMap[color]}`,
//       shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
//       iconSize: [25, 41],
//       iconAnchor: [12, 41],
//       popupAnchor: [1, -34],
//       shadowSize: [41, 41]
//     });
//   }

//   protected isDriverRide(ride: any): ride is DriverRide {
//     return 'status' in ride && 'stopStatuses' in ride;
//   }

//   protected refreshRides(): void {
//     this.loadData();
//   }
// }