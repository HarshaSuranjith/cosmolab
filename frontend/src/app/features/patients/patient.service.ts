import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PatientResponse, PatientQueryParams } from '../../core/models/patient.model';
import { PagedResponse } from '../../core/models/paged-response.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/patients';

  getPatients(query: PatientQueryParams = {}): Observable<PagedResponse<PatientResponse>> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 20));
    if (query.search) params = params.set('search', query.search);
    if (query.ward)   params = params.set('ward',   query.ward);
    if (query.status) params = params.set('status', query.status);
    return this.http.get<PagedResponse<PatientResponse>>(this.base, { params });
  }

  getPatient(id: string): Observable<PatientResponse> {
    return this.http.get<PatientResponse>(`${this.base}/${id}`);
  }
}
