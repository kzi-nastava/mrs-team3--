import {AfterViewInit, Component, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {Ride} from './incoming-rides.model';
import {IncomingRidesService} from '../services/incoming-rides.service';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-incoming-rides',
  standalone: true,
  imports: [
    FormsModule, CommonModule
  ],
  templateUrl: './incoming-rides.html',
  styleUrls: ['./incoming-rides.css'],
})
export class IncomingRides implements OnInit, AfterViewInit  {
  protected rides = signal<Ride[]>([]);
  protected selectedRide = signal<Ride | null>(null);
  startDate: string = '';

  constructor(
    private incomingRidesService: IncomingRidesService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRides();
  }

  ngAfterViewInit(): void {
  }

  private loadRides(): void {
    this.incomingRidesService.getRides().subscribe({
      next: (rows: Ride[]) => {
        this.rides.set(rows ?? []);
      },
      error: err => console.error('getRides failed:', err),
    });
  }

  canCancel(ride: Ride): boolean {
    const startMs = new Date(ride.startTime).getTime();
    if (Number.isNaN(startMs)) return false;

    const diffMs = startMs - Date.now();
    const tenMinMs = 10 * 60 * 1000;

    return diffMs >= tenMinMs;
  }
  cancelRide(event: Event, ride: Ride) {
    event.stopPropagation();

    if (!this.canCancel(ride))
      return;

    this.incomingRidesService.cancelIncomingRide(ride.id).subscribe({
      next: () => {
        this.loadRides();
      },
      error: err => console.error('cancelIncomingRide failed:', err),
    });
  }

  minutesToStart(ride: Ride): number {
    const startMs = new Date(ride.startTime).getTime();
    if (Number.isNaN(startMs)) return 0;

    return Math.max(0, Math.floor((startMs - Date.now()) / 60000));
  }

  formatDateTime(dateString: string): string {
    const d = new Date(dateString);
    if (Number.isNaN(d.getTime())) return '-';

    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
