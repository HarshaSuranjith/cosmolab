export type FlagLevel = 'normal' | 'amber' | 'red';

export interface VitalFlag {
  level: FlagLevel;
  icon: string;
}

export function systolicBpFlag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v > 180 || v < 80) return 'red';
  if (v > 140 || v < 90) return 'amber';
  return 'normal';
}

export function diastolicBpFlag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v > 90 || v < 60) return 'amber';
  return 'normal';
}

export function heartRateFlag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v > 140 || v < 40) return 'red';
  if (v > 100 || v < 60) return 'amber';
  return 'normal';
}

export function respiratoryRateFlag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v > 20 || v < 12) return 'amber';
  return 'normal';
}

export function temperatureFlag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v > 39.0) return 'red';
  if (v > 37.2 || v < 36.1) return 'amber';
  return 'normal';
}

export function spo2Flag(v: number | null): FlagLevel {
  if (v === null) return 'normal';
  if (v < 93) return 'red';
  if (v < 95) return 'amber';
  return 'normal';
}

export function flagIcon(level: FlagLevel): string {
  if (level === 'red') return 'error_outline';
  if (level === 'amber') return 'warning';
  return '';
}

export function isCritical(flags: string[]): boolean {
  return flags.some(f =>
    f.includes('HIGH') ||
    f.includes('LOW') ||
    f.includes('CRITICAL'),
  );
}
