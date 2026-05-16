export interface EhrResponse {
  ehrId: string;
  subjectId: string;
  systemId: string;
  createdAt: string;
  status: 'ACTIVE' | 'INACTIVE';
}
