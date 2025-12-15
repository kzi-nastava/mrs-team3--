import { Component, OnInit, OnDestroy } from '@angular/core';
import { SidebarComponent } from '../sidebar/sidebar';
import { RideBookingComponent } from '../ride-booking/ride-booking';
import { MapComponent } from '../map/map';
import { ProfileComponent } from '../profile/profile';
import { NavigationService } from '../services/navigation.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [SidebarComponent, RideBookingComponent, MapComponent, ProfileComponent],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css'
})
export class LandingPageComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  currentView: 'home' | 'profile' = 'home';
  
  constructor(private navigationService: NavigationService) {}

  ngOnInit(): void {
    this.navigationService.currentView$
      .pipe(takeUntil(this.destroy$))
      .subscribe(view => {
        this.currentView = view;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}