import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { replaceUrl: true });
    //router.navigate(['/go to error page']);
    return false;
  }

  // Check for role requirement
  const requiredRole = route.data['role'] as string;
  if (requiredRole && !authService.hasRole(requiredRole)) {
    // User doesn't have the required role
    router.navigate(['/'], { replaceUrl: true });
    return false;
  }

  return true;
};

// Role-specific guards
export const passengerGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

  if (!authService.isPassenger()) {
    router.navigate(['/'], { replaceUrl: true });
    return false;
  }

  return true;
};

export const driverGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

  if (!authService.isDriver()) {
    router.navigate(['/'], { replaceUrl: true });
    return false;
  }

  return true;
};

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

  if (!authService.isAdmin()) {
    router.navigate(['/'], { replaceUrl: true });
    return false;
  }

  return true;
};

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // If user is already logged in, block /login and /register
  if (authService.isAuthenticated()) {
    router.navigate(['/'], { replaceUrl: true });
    return false;
  }

  return true;
};
