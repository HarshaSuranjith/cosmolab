import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProblemResponse, ProblemRequest } from '../../../core/models/problem.model';

@Injectable({ providedIn: 'root' })
export class ProblemListService {
  private readonly http = inject(HttpClient);

  getProblems(ehrId: string, status?: string): Observable<ProblemResponse[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ProblemResponse[]>(`/api/v1/ehr/${ehrId}/problems`, { params });
  }

  createProblem(ehrId: string, body: ProblemRequest): Observable<ProblemResponse> {
    return this.http.post<ProblemResponse>(`/api/v1/ehr/${ehrId}/problems`, body);
  }
}
