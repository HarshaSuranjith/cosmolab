export interface PatientResponse {
  id: string;
  firstName: string;
  lastName: string;
  personalNumber: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  ward: string;
  status: 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED';
  createdAt: string;
  updatedAt: string;
}

export interface PatientQueryParams {
  search?: string;
  ward?: string;
  status?: string;
  page?: number;
  size?: number;
}
