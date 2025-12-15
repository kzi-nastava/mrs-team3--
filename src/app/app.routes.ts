import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'profile', component: ProfileComponent },
  { path: '**', redirectTo: '' }
];