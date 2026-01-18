import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';
import { Login } from './login/login';
import { RegisterComponent } from './register/register';
import { ResetPasswordComponent } from './reset-password/reset-password';
import { ForgotPassword } from './forgot-password/forgot-password';
import {VerificationResultComponent} from './verification-result/verification-result';
import { ErrorComponent } from './error/error';
import { DriverHistoryComponent } from './ride-history/driver/driver-history';  
import { PassengerHistoryComponent } from './ride-history/passenger/passenger-history';
import { authGuard } from './guard/auth.guard';
export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'login', component: Login },
  { path: 'register', component: RegisterComponent },
  

  {
    path: 'driver-register',
    loadComponent: () =>
      import('./driver-register/driver-register')
        .then(m => m.DriverRegisterComponent),
        canActivate: [authGuard]
  },
  // {path: 'driver-history', component: DriverHistoryComponent, canActivate: [authGuard], data: { role: 'DRIVER' }},
  {path: 'driver-history', component: DriverHistoryComponent},
  { path: 'passenger-history', component: PassengerHistoryComponent, canActivate: [authGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', component: ForgotPassword },
  {path: 'verification-result', component: VerificationResultComponent},
  { path: '**', component: ErrorComponent }
];
