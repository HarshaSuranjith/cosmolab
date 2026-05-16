export interface ProblemResponse {
  id: string;
  ehrId: string;
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  status: 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED';
  onsetDate: string | null;
  resolvedDate: string | null;
  recordedAt: string;
}

export interface ProblemRequest {
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE';
  status: 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED';
  onsetDate?: string;
}
