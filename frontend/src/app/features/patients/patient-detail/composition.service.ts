import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompositionResponse, CompositionRequest, CompositionQueryParams } from '../../../core/models/composition.model';
import { PagedResponse } from '../../../core/models/paged-response.model';

@Injectable({ providedIn: 'root' })
export class CompositionService {
  private readonly http = inject(HttpClient);

  getCompositions(ehrId: string, query: CompositionQueryParams = {}): Observable<PagedResponse<CompositionResponse>> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 10));
    if (query.type) params = params.set('type', query.type);
    return this.http.get<PagedResponse<CompositionResponse>>(`/api/v1/ehr/${ehrId}/compositions`, { params });
  }

  createComposition(ehrId: string, body: CompositionRequest): Observable<CompositionResponse> {
    return this.http.post<CompositionResponse>(`/api/v1/ehr/${ehrId}/compositions`, body);
  }
}
