import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';
import { Login } from './login/login';
import { RideHistoryComponent } from './ride-history/ride-history';
import { RegisterComponent } from './register/register';
import {ChangePasswordComponent} from './change-password/change-password';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'profile', component: ProfileComponent },
  {path : "login", component: Login},
  {path: "register", component: RegisterComponent},
  { path: 'ride-history', component: RideHistoryComponent },
  {path: "change-password", component: ChangePasswordComponent},
  { path: '**', redirectTo: '' }
];
