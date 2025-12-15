import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  private currentViewSubject = new BehaviorSubject<'home' | 'profile'>('home');
  public currentView$ = this.currentViewSubject.asObservable();

  navigateToHome(): void {
    this.currentViewSubject.next('home');
  }

  navigateToProfile(): void {
    this.currentViewSubject.next('profile');
  }
}