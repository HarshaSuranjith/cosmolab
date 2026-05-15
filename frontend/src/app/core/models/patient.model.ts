export type PatientStatus = 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface PatientResponse {
  id: string;
  firstName: string;
  lastName: string;
  personalNumber: string;
  dateOfBirth: string;
  gender: Gender;
  ward: string;
  status: PatientStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PatientQueryParams {
  ward?: string;
  status?: PatientStatus;
  search?: string;
  page?: number;
  size?: number;
}
