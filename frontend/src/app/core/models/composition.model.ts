export type CompositionType =
  | 'ENCOUNTER_NOTE'
  | 'ADMISSION'
  | 'PROGRESS_NOTE'
  | 'DISCHARGE_SUMMARY';

export type CompositionStatus = 'COMPLETE' | 'INCOMPLETE' | 'AMENDED';

export interface CompositionResponse {
  id: string;
  ehrId: string;
  type: CompositionType;
  authorId: string;
  startTime: string;
  commitTime: string;
  facilityName: string;
  status: CompositionStatus;
}

export interface CompositionRequest {
  type: CompositionType;
  facilityName: string;
  startTime: string;
}

export interface CompositionQueryParams {
  type?: CompositionType;
  page?: number;
  size?: number;
}
