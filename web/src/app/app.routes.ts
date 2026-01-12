import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';
import { Login } from './login/login';
import { RideHistoryComponent } from './ride-history/ride-history';
import { RegisterComponent } from './register/register';
import { ResetPasswordComponent } from './reset-password/reset-password';
import { ForgotPassword } from './forgot-password/forgot-password';
import {VerificationResultComponent} from './verification-result/verification-result';
export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'login', component: Login },
  { path: 'register', component: RegisterComponent },

  {
    path: 'driver-register',
    loadComponent: () =>
      import('./driver-register/driver-register')
        .then(m => m.DriverRegisterComponent)
  },

  { path: 'ride-history', component: RideHistoryComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', component: ForgotPassword },
  {path: 'verification-result', component: VerificationResultComponent},

  { path: '**', redirectTo: '' }
];
