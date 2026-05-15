import { Component, Input, OnChanges } from '@angular/core';
import { Color, LegendPosition, ScaleType } from '@swimlane/ngx-charts';
import { VitalSignsResponse } from '../../../core/models/vital-signs.model';
import { VitalsRangeUtil, VitalFlag } from '../../../core/utils/vitals-range.util';

interface ChartSeries {
  name: string;
  series: { name: Date; value: number }[];
}

@Component({
  selector: 'app-vital-signs-chart',
  templateUrl: './vital-signs-chart.component.html',
  styleUrls: ['./vital-signs-chart.component.scss']
})
export class VitalSignsChartComponent implements OnChanges {
  @Input() readings: VitalSignsResponse[] = [];
  @Input() compact = false;

  chartData: ChartSeries[] = [];
  readonly legendPosition = LegendPosition.Below;
  readonly colorScheme: Color = {
    name: 'cosmolab',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#009688', '#F57C00', '#5C6BC0', '#C62828', '#2E7D32', '#F9A825']
  };

  get latest(): VitalSignsResponse | null {
    return this.readings.length > 0 ? this.readings[0] : null;
  }

  ngOnChanges(): void {
    if (!this.compact) {
      this.buildChartData();
    }
  }

  private buildChartData(): void {
    const sliced = [...this.readings].reverse().slice(-50);

    const series: Array<{ name: string; field: keyof VitalSignsResponse }> = [
      { name: 'Systolic BP', field: 'systolicBp' },
      { name: 'Heart Rate',  field: 'heartRate' },
      { name: 'Resp Rate',   field: 'respiratoryRate' },
      { name: 'Temperature', field: 'temperature' },
      { name: 'SpO₂',        field: 'oxygenSaturation' },
      { name: 'Weight',      field: 'weight' }
    ];

    this.chartData = series
      .map(s => ({
        name: s.name,
        series: sliced
          .filter(r => r[s.field] !== null)
          .map(r => ({ name: new Date(r.recordedAt), value: r[s.field] as number }))
      }))
      .filter(s => s.series.length > 0);
  }

  flagFor(field: keyof VitalSignsResponse, value: number | null): VitalFlag {
    switch (field) {
      case 'systolicBp':      return VitalsRangeUtil.systolicBp(value);
      case 'diastolicBp':     return VitalsRangeUtil.diastolicBp(value);
      case 'heartRate':       return VitalsRangeUtil.heartRate(value);
      case 'respiratoryRate': return VitalsRangeUtil.respiratoryRate(value);
      case 'temperature':     return VitalsRangeUtil.temperature(value);
      case 'oxygenSaturation':return VitalsRangeUtil.oxygenSaturation(value);
      default: return { level: 'normal', cssClass: 'vital-normal', iconName: null };
    }
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' });
  }
}
