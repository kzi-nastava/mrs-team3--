import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';
import { Login } from './login/login';
import { RideHistoryComponent } from './ride-history/ride-history';
import { RegisterComponent } from './register/register';
import { ResetPasswordComponent } from './reset-password/reset-password';
import { ForgotPassword } from './forgot-password/forgot-password';
import {VerificationResultComponent} from './verification-result/verification-result';
import {authGuard, guestGuard} from './guard/auth.guard';
export const routes: Routes = [
  { path: '', component: LandingPageComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },


  {
    path: 'driver-register',
    loadComponent: () =>
      import('./driver-register/driver-register')
        .then(m => m.DriverRegisterComponent),
        canActivate: [authGuard]
  },

  { path: 'ride-history', component: RideHistoryComponent, canActivate: [authGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', component: ForgotPassword },
  {path: 'verification-result', component: VerificationResultComponent},

  { path: '**', redirectTo: '' }
];
