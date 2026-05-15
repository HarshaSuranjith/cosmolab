export interface EhrResponse {
  ehrId: string;
  subjectId: string;
  systemId: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}
