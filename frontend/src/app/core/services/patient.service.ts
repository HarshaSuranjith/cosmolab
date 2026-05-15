import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PatientQueryParams, PatientResponse } from '../models/patient.model';
import { PagedResponse } from '../models/paged-response.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly base = '/api/v1/patients';

  constructor(private http: HttpClient) {}

  getPatients(params: PatientQueryParams = {}): Observable<PagedResponse<PatientResponse>> {
    let httpParams = new HttpParams();
    if (params.ward)   httpParams = httpParams.set('ward', params.ward);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    return this.http.get<PagedResponse<PatientResponse>>(this.base, { params: httpParams });
  }

  getPatient(id: string): Observable<PatientResponse> {
    return this.http.get<PatientResponse>(`${this.base}/${id}`);
  }
}
