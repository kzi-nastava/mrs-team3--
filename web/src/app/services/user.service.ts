import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private baseUrl = 'http://localhost:8080/api/admin/users';

  constructor(private http: HttpClient) {}

  blockUser(userId: number, blocked: boolean, reason?: string) {
    return this.http.put(
      `${this.baseUrl}/${userId}/block`,
      {
        blocked: blocked,
        reason: reason ?? null
      }
    );
  }

}
