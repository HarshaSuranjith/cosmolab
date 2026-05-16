import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { VitalSignsResponse } from '../../core/models/vital-signs.model';
import {
  systolicBpFlag, heartRateFlag as hrFlag,
  respiratoryRateFlag, temperatureFlag, spo2Flag,
  flagIcon, FlagLevel,
} from '../utils/vital-flags.util';

interface ChartPoint { x: number; y: number; }
interface ChartSeries { label: string; points: ChartPoint[]; color: string; }

@Component({
  selector: 'app-vital-signs-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './vital-signs-chart.component.html',
  styleUrl: './vital-signs-chart.component.scss',
})
export class VitalSignsChartComponent {
  readonly readings = input<VitalSignsResponse[]>([]);
  readonly compact  = input<boolean>(false);

  protected readonly latest = computed(() => this.readings()[0] ?? null);

  // Computed flags for the compact latest-reading display
  protected readonly bpFlag   = computed(() => systolicBpFlag(this.latest()?.systolicBp ?? null));
  protected readonly hrFlag   = computed(() => hrFlag(this.latest()?.heartRate ?? null));
  protected readonly tempFlag = computed(() => temperatureFlag(this.latest()?.temperature ?? null));
  protected readonly spFlag   = computed(() => spo2Flag(this.latest()?.oxygenSaturation ?? null));
  protected readonly rrFlag   = computed(() => respiratoryRateFlag(this.latest()?.respiratoryRate ?? null));

  // Methods exposed to template for per-row flag computation in the table
  protected getBpFlag(v: number | null):   FlagLevel { return systolicBpFlag(v); }
  protected getHrFlag(v: number | null):   FlagLevel { return hrFlag(v); }
  protected getTempFlag(v: number | null): FlagLevel { return temperatureFlag(v); }
  protected getSpFlag(v: number | null):   FlagLevel { return spo2Flag(v); }

  protected flagIcon(level: FlagLevel): string { return flagIcon(level); }

  protected colorFor(level: FlagLevel): string {
    if (level === 'red')   return 'var(--nc-error)';
    if (level === 'amber') return 'var(--nc-severity-moderate)';
    return 'var(--nc-on-surface)';
  }

  // ─── SVG sparklines (full mode) ───────────────────────────────────────────
  protected readonly chartData = computed((): ChartSeries[] => {
    const rs = [...this.readings()].reverse(); // oldest first
    if (rs.length < 2) return [];
    const n = rs.length;
    const W = 480, H = 220;

    const seriesDefs: { label: string; color: string; fn: (r: VitalSignsResponse) => number | null }[] = [
      { label: 'Systolic BP', color: '#00342b', fn: r => r.systolicBp },
      { label: 'Heart Rate',  color: '#F9A825', fn: r => r.heartRate },
      { label: 'SpO₂ %',     color: '#2196F3', fn: r => r.oxygenSaturation },
      { label: 'Temp °C',    color: '#ba1a1a', fn: r => r.temperature },
    ];

    return seriesDefs
      .map(({ label, color, fn }) => {
        const vals = rs.map(fn).filter((v): v is number => v !== null);
        if (vals.length < 2) return null;
        const minV = Math.min(...vals), maxV = Math.max(...vals);
        const range = maxV - minV || 1;
        const points: ChartPoint[] = rs
          .map((r, i) => ({ v: fn(r), i }))
          .filter((x): x is { v: number; i: number } => x.v !== null)
          .map(({ v, i }) => ({
            x: (i / (n - 1)) * W,
            y: H - ((v - minV) / range) * (H - 20) - 10,
          }));
        return { label, color, points };
      })
      .filter((s): s is ChartSeries => s !== null);
  });

  protected pointsStr(pts: ChartPoint[]): string {
    return pts.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  }

  protected readonly tableReadings = computed(() => this.readings().slice(0, 10));
}
