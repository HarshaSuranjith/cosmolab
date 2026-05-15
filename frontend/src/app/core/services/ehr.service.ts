import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EhrResponse } from '../models/ehr.model';

@Injectable({ providedIn: 'root' })
export class EhrService {
  private readonly base = '/api/v1/ehr';

  constructor(private http: HttpClient) {}

  getEhrBySubject(patientId: string): Observable<EhrResponse> {
    return this.http.get<EhrResponse>(`${this.base}/subject/${patientId}`);
  }
}
