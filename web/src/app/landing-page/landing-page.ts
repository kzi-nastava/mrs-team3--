import { Component } from '@angular/core';
import {Observable} from 'rxjs';
import {AsyncPipe} from '@angular/common';

import { RideBookingComponent } from '../ride-booking/ride-booking';
import { MapComponent } from '../map/map';
import {RideFormComponent} from '../ride-form/ride-form';
import {AuthService} from '../services/auth.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [AsyncPipe, RideBookingComponent, MapComponent, RideFormComponent],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css'
})
export class LandingPageComponent {
  user$: Observable<any>;

  constructor(private auth: AuthService) {
    this.user$ = this.auth.currentUser$;
  }
}
