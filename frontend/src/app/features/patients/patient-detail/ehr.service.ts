import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EhrResponse } from '../../../core/models/ehr.model';

@Injectable({ providedIn: 'root' })
export class EhrService {
  private readonly http = inject(HttpClient);

  getEhrBySubject(patientId: string): Observable<EhrResponse> {
    return this.http.get<EhrResponse>(`/api/v1/ehr/subject/${patientId}`);
  }
}
