export type VitalFlagLevel = 'normal' | 'amber' | 'red';

export interface VitalFlag {
  level: VitalFlagLevel;
  cssClass: string;
  iconName: string | null;
}

const NORMAL: VitalFlag = { level: 'normal', cssClass: 'vital-normal', iconName: null };
const AMBER: VitalFlag  = { level: 'amber',  cssClass: 'vital-amber',  iconName: 'warning' };
const RED: VitalFlag    = { level: 'red',     cssClass: 'vital-red',    iconName: 'error' };

export class VitalsRangeUtil {
  static systolicBp(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v > 180 || v < 80) return RED;
    if (v > 140 || v < 90) return AMBER;
    return NORMAL;
  }

  static diastolicBp(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v > 90 || v < 60) return AMBER;
    return NORMAL;
  }

  static heartRate(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v > 140 || v < 40) return RED;
    if (v > 100 || v < 60) return AMBER;
    return NORMAL;
  }

  static respiratoryRate(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v > 20 || v < 12) return AMBER;
    return NORMAL;
  }

  static temperature(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v > 39.0) return RED;
    if (v > 37.2 || v < 36.1) return AMBER;
    return NORMAL;
  }

  static oxygenSaturation(v: number | null): VitalFlag {
    if (v === null) return NORMAL;
    if (v < 93) return RED;
    if (v < 95) return AMBER;
    return NORMAL;
  }

  static isCritical(vitals: { systolicBp?: number | null; heartRate?: number | null; temperature?: number | null; oxygenSaturation?: number | null }): boolean {
    return (
      (vitals.oxygenSaturation !== null && vitals.oxygenSaturation !== undefined && vitals.oxygenSaturation < 93) ||
      (vitals.systolicBp !== null && vitals.systolicBp !== undefined && (vitals.systolicBp > 180 || vitals.systolicBp < 80)) ||
      (vitals.heartRate !== null && vitals.heartRate !== undefined && (vitals.heartRate > 140 || vitals.heartRate < 40)) ||
      (vitals.temperature !== null && vitals.temperature !== undefined && vitals.temperature > 39.0)
    );
  }
}
