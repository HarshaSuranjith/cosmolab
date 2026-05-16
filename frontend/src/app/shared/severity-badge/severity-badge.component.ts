import { ChangeDetectionStrategy, Component, input } from '@angular/core';

type Severity = 'MILD' | 'MODERATE' | 'SEVERE' | 'ROUTINE' | 'URGENT' | 'CRITICAL';

@Component({
  selector: 'app-severity-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './severity-badge.component.scss',
  template: `
    <span class="severity-chip"
          [class]="'severity-' + severity().toLowerCase()"
          [attr.data-testid]="'severity-badge-' + severity()">
      <span class="material-symbols-outlined chip-icon">{{ iconFor(severity()) }}</span>
      {{ severity() }}
    </span>
  `,
})
export class SeverityBadgeComponent {
  readonly severity = input.required<Severity>();

  protected iconFor(s: Severity): string {
    if (s === 'SEVERE' || s === 'CRITICAL') return 'error';
    if (s === 'MODERATE' || s === 'URGENT') return 'warning';
    return 'check_circle';
  }
}
