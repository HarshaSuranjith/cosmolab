export interface VitalSignsResponse {
  id: string;
  compositionId: string;
  recordedAt: string;
  recordedBy: string;
  systolicBp: number | null;
  diastolicBp: number | null;
  heartRate: number | null;
  respiratoryRate: number | null;
  temperature: number | null;
  oxygenSaturation: number | null;
  weight: number | null;
}

export interface VitalSignsRequest {
  recordedAt: string;
  systolicBp?: number;
  diastolicBp?: number;
  heartRate?: number;
  respiratoryRate?: number;
  temperature?: number;
  oxygenSaturation?: number;
  weight?: number;
}

export interface VitalsQueryParams {
  from?: string;
  to?: string;
}
