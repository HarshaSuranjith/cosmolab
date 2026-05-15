export type Severity = 'MILD' | 'MODERATE' | 'SEVERE';
export type ProblemStatus = 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED';

export interface ProblemResponse {
  id: string;
  ehrId: string;
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: Severity;
  status: ProblemStatus;
  onsetDate: string | null;
  resolvedDate: string | null;
  recordedAt: string;
}

export interface ProblemRequest {
  compositionId: string;
  icd10Code: string;
  displayName: string;
  severity: Severity;
  status: ProblemStatus;
  onsetDate?: string;
}
