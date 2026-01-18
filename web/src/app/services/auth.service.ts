import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { RegisterRequest } from '../register/register.model';
import { Router } from '@angular/router';
import { env } from '../../env/env';

interface LoginResponse {
  id: number;
  email: string;
  role: string;
  token: string;
}

interface DecodedToken {
  sub: string;      // email
  uid: number;      // user id
  role: string;     // role (PASSENGER, DRIVER, ADMIN)
  iss: string;      // issuer
  iat: number;      // issued at
  exp: number;      // expires at
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = env.API_URL +"/api/auth";
  private tokenKey = 'token';

  // Observable for components to subscribe to user changes
  private currentUserSubject = new BehaviorSubject<DecodedToken | null>(this.getDecodedToken());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      email: email,
      password: password
    }).pipe(
      tap(response => {
        // Save token to localStorage
        this.setToken(response.token);
        // Update current user observable
        this.currentUserSubject.next(this.getDecodedToken());
      })
    );
  }

  registerPassenger(userData: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email: email });
  }

  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, {
      token: token,
      newPassword: newPassword
    });
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login'], { replaceUrl: true }).then(() => window.location.reload());
  }

  // Token management
  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  // Decode JWT token manually (without external library)
  getDecodedToken(): DecodedToken | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return decoded as DecodedToken;
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  // User information getters
  getUserId(): number | null {
    const decoded = this.getDecodedToken();
    return decoded?.uid || null;
  }

  getUserRole(): string | null {
    const decoded = this.getDecodedToken();
    return decoded?.role || null;
  }

  getUserEmail(): string | null {
    const decoded = this.getDecodedToken();
    return decoded?.sub || null;
  }

  // Authentication checks
  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    const decoded = this.getDecodedToken();
    if (!decoded) return false;

    // Check if token is expired
    const now = Math.floor(Date.now() / 1000);
    return decoded.exp > now;
  }

  // Role-based checks
  hasRole(role: string): boolean {
    return this.getUserRole() === role;
  }

  isPassenger(): boolean {
    return this.hasRole('PASSENGER');
  }

  isDriver(): boolean {
    return this.hasRole('DRIVER');
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  // Get current user data
  getCurrentUser(): DecodedToken | null {
    return this.getDecodedToken();
  }
}
