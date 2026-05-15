import { Component, Input } from '@angular/core';
import { Severity } from '../../../core/models/problem.model';

interface BadgeConfig {
  background: string;
  color: string;
  icon: string;
}

const CONFIGS: Record<Severity, BadgeConfig> = {
  MILD:     { background: '#E8F5E9', color: '#2E7D32', icon: 'check_circle' },
  MODERATE: { background: '#FFF8E1', color: '#F9A825', icon: 'warning' },
  SEVERE:   { background: '#FFEBEE', color: '#C62828', icon: 'error' }
};

@Component({
  selector: 'app-severity-badge',
  template: `
    <mat-chip
      [style.background]="config.background"
      [style.color]="config.color"
      [attr.data-testid]="'severity-badge-' + severity"
      class="severity-chip">
      <mat-icon matChipAvatar [style.color]="config.color">{{ config.icon }}</mat-icon>
      {{ severity }}
    </mat-chip>
  `,
  styles: [`
    .severity-chip {
      font-size: 11px;
      font-weight: 500;
      height: 24px;
      min-height: 24px;
    }
  `]
})
export class SeverityBadgeComponent {
  @Input() severity: Severity = 'MILD';

  get config(): BadgeConfig {
    return CONFIGS[this.severity] ?? CONFIGS['MILD'];
  }
}
