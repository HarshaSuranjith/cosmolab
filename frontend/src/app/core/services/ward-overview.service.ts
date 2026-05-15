import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WardOverviewResponse } from '../models/ward-overview.model';

@Injectable({ providedIn: 'root' })
export class WardOverviewService {
  constructor(private http: HttpClient) {}

  getOverview(wardId: string): Observable<WardOverviewResponse> {
    return this.http.get<WardOverviewResponse>(`/api/v1/ward/${wardId}/overview`);
  }
}
