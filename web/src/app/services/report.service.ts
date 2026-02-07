import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { env } from '../../env/env';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  private api = env.API_URL + '/api/rides/reports';
  private baseUrl = env.API_URL;

  constructor(private http: HttpClient) { }

  getReport(from: string, to: string, userId?: number | null) {
  let params = new HttpParams()
    .set('from', from)
    .set('to', to);

  if (userId) {
    params = params.set('userId', userId);
  }

  return this.http.get<any>(this.api, { params });
}


  getAllUsers() {
    return this.http.get<any[]>(`${this.baseUrl}/api/admin/users`);
  }
}
