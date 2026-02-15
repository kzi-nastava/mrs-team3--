import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DriverStatusComponent {
  private inRideSubject = new BehaviorSubject<boolean>(false);
  inRide$ = this.inRideSubject.asObservable();

  private activeSubject = new BehaviorSubject<boolean>(true);
  active$ = this.activeSubject.asObservable();

  setInRide(v: boolean) { this.inRideSubject.next(v); }
  setActive(v: boolean) { this.activeSubject.next(v); }
}

