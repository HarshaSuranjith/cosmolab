import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompositionQueryParams, CompositionRequest, CompositionResponse } from '../models/composition.model';
import { PagedResponse } from '../models/paged-response.model';

@Injectable({ providedIn: 'root' })
export class CompositionService {
  constructor(private http: HttpClient) {}

  getCompositions(ehrId: string, params: CompositionQueryParams = {}): Observable<PagedResponse<CompositionResponse>> {
    let httpParams = new HttpParams();
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    return this.http.get<PagedResponse<CompositionResponse>>(`/api/v1/ehr/${ehrId}/compositions`, { params: httpParams });
  }

  createComposition(ehrId: string, body: CompositionRequest): Observable<CompositionResponse> {
    return this.http.post<CompositionResponse>(`/api/v1/ehr/${ehrId}/compositions`, body);
  }
}
