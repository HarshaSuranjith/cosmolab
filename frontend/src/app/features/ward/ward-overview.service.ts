import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WardOverviewResponse } from '../../core/models/ward-overview.model';

@Injectable({ providedIn: 'root' })
export class WardOverviewService {
  private readonly http = inject(HttpClient);

  getOverview(wardId: string): Observable<WardOverviewResponse> {
    return this.http.get<WardOverviewResponse>(`/api/v1/ward/${wardId}/overview`);
  }
}
