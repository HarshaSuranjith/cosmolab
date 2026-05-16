import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VitalSignsResponse, VitalSignsRequest, VitalsQueryParams } from '../../../core/models/vital-signs.model';

@Injectable({ providedIn: 'root' })
export class VitalSignsService {
  private readonly http = inject(HttpClient);

  getVitals(ehrId: string, query: VitalsQueryParams = {}): Observable<VitalSignsResponse[]> {
    let params = new HttpParams();
    if (query.from) params = params.set('from', query.from);
    if (query.to)   params = params.set('to',   query.to);
    return this.http.get<VitalSignsResponse[]>(`/api/v1/ehr/${ehrId}/vitals`, { params });
  }

  getLatestVitals(ehrId: string): Observable<VitalSignsResponse> {
    return this.http.get<VitalSignsResponse>(`/api/v1/ehr/${ehrId}/vitals/latest`);
  }

  createVitals(ehrId: string, compositionId: string, body: VitalSignsRequest): Observable<VitalSignsResponse> {
    return this.http.post<VitalSignsResponse>(
      `/api/v1/ehr/${ehrId}/compositions/${compositionId}/vitals`, body,
    );
  }
}
