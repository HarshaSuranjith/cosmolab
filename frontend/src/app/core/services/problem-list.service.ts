import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProblemRequest, ProblemResponse, ProblemStatus } from '../models/problem.model';

@Injectable({ providedIn: 'root' })
export class ProblemListService {
  constructor(private http: HttpClient) {}

  getProblems(ehrId: string, status?: ProblemStatus): Observable<ProblemResponse[]> {
    let httpParams = new HttpParams();
    if (status) httpParams = httpParams.set('status', status);
    return this.http.get<ProblemResponse[]>(`/api/v1/ehr/${ehrId}/problems`, { params: httpParams });
  }

  createProblem(ehrId: string, body: ProblemRequest): Observable<ProblemResponse> {
    return this.http.post<ProblemResponse>(`/api/v1/ehr/${ehrId}/problems`, body);
  }
}
