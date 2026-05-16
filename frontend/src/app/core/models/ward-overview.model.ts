export interface WardOverviewResponse {
  wardId: string;
  patientCount: number;
  patients: PatientSummary[];
}

export interface PatientSummary {
  patientId: string;
  ehrId: string;
  firstName: string;
  lastName: string;
  status: 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED';
  latestVitals: LatestVitals | null;
  activeProblemCount: number;
  flags: string[];
}

export interface LatestVitals {
  recordedAt: string;
  systolicBp: number | null;
  diastolicBp: number | null;
  heartRate: number | null;
  temperature: number | null;
  oxygenSaturation: number | null;
}
