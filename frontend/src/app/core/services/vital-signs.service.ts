import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VitalSignsRequest, VitalSignsResponse, VitalsQueryParams } from '../models/vital-signs.model';

@Injectable({ providedIn: 'root' })
export class VitalSignsService {
  constructor(private http: HttpClient) {}

  getVitals(ehrId: string, params: VitalsQueryParams = {}): Observable<VitalSignsResponse[]> {
    let httpParams = new HttpParams();
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to)   httpParams = httpParams.set('to', params.to);
    return this.http.get<VitalSignsResponse[]>(`/api/v1/ehr/${ehrId}/vitals`, { params: httpParams });
  }

  getLatestVitals(ehrId: string): Observable<VitalSignsResponse | null> {
    return this.http.get<VitalSignsResponse | null>(`/api/v1/ehr/${ehrId}/vitals/latest`);
  }

  createVitals(ehrId: string, compositionId: string, body: VitalSignsRequest): Observable<VitalSignsResponse> {
    return this.http.post<VitalSignsResponse>(
      `/api/v1/ehr/${ehrId}/compositions/${compositionId}/vitals`,
      body
    );
  }
}
