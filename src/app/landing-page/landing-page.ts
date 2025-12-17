import { Component } from '@angular/core';
import { RideBookingComponent } from '../ride-booking/ride-booking';
import { MapComponent } from '../map/map';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RideBookingComponent, MapComponent],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css'
})
export class LandingPageComponent {}
