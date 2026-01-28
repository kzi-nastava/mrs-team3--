import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page';
import { ProfileComponent } from './profile/profile';
import { Login } from './login/login';
import { RegisterComponent } from './register/register';
import { ResetPasswordComponent } from './reset-password/reset-password';
import { ForgotPassword } from './forgot-password/forgot-password';
import {VerificationResultComponent} from './verification-result/verification-result';
import {adminGuard, authGuard, driverGuard, guestGuard, passengerGuard} from './guard/auth.guard';
import { ErrorComponent } from './error/error';
import { DriverHistoryComponent } from './ride-history/driver/driver-history';
import { PassengerHistoryComponent } from './ride-history/passenger/passenger-history';
import { AdminNotificationsComponent } from './notifications/admin-notifications/admin-notifications';
import { DriverNotificationsComponent } from './notifications/driver-notifications/driver-notifications';
import { PassengerNotificationsComponent } from './notifications/passenger-notifications/passenger-notifications';
import { RideReviewComponent } from './ride-review/ride-review';
import { RideTrackingComponent } from './ride-tracking/ride-tracking';
import { DriverDashboardComponent } from './driver-dashboard/driver-dashboard';
import {IncomingRides} from './incoming-rides/incoming-rides';

export const routes: Routes = [

  { path: '', component: LandingPageComponent },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'verification-result', component: VerificationResultComponent },

  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },

  {
    path: 'driver-register',
    loadComponent: () =>
      import('./driver-register/driver-register')
        .then(m => m.DriverRegisterComponent),
    canActivate: [authGuard]
  },

  { path: 'driver-history', component: DriverHistoryComponent, canActivate: [driverGuard] },
  { path: 'passenger-history', component: PassengerHistoryComponent, canActivate: [passengerGuard] },

  {
    path: 'admin/profile-change-requests',
    loadComponent: () =>
      import('./admin-profile-change-list/admin-profile-change-list')
        .then(m => m.AdminProfileChangeList),
    canActivate: [authGuard],
    data: { role: 'ADMIN' }
  },
  { path: 'ride-tracking', component: RideTrackingComponent, canActivate: [passengerGuard] },
  {path: 'ride-tracking/:token', component: RideTrackingComponent},
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', component: ForgotPassword },
  {path: 'verification-result', component: VerificationResultComponent},
  { path: 'admin-notifications', component: AdminNotificationsComponent, canActivate: [adminGuard]},
  { path: 'driver-notifications', component: DriverNotificationsComponent, canActivate: [driverGuard]},
  { path: 'passenger-notifications', component: PassengerNotificationsComponent, canActivate: [passengerGuard]},
  {path: 'ride-review/:id', component: RideReviewComponent, canActivate: [passengerGuard]},
  { path: 'driver-dashboard', component: DriverDashboardComponent, canActivate: [driverGuard] },
  {path: 'incoming-rides', component: IncomingRides, canActivate: [passengerGuard]},
  { path: '**', component: ErrorComponent }
];
