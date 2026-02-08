import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminUser {
    id: number;
    name: string;
    surname: string;
    email: string;
    phoneNumber: string;
    address: string;
    role: string;
    blocked: boolean;
}

export interface ActiveDriver {
    id: number;
    name: string;
    surname: string;
    email: string;
    blocked: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class AdminUserService {

    private api = 'http://localhost:8080/api/admin';

    constructor(private http: HttpClient) { }

    getAllUsers(): Observable<AdminUser[]> {
        return this.http.get<AdminUser[]>(`${this.api}/users/details`);
    }

    getActiveDrivers(): Observable<ActiveDriver[]> {
        return this.http.get<ActiveDriver[]>(`${this.api}/drivers/active`);
    }


}
