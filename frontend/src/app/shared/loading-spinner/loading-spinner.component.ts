import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatProgressSpinnerModule],
  styleUrl: './loading-spinner.component.scss',
  template: `
    <div class="spinner-wrap" [class.overlay]="overlay()" data-testid="loading-spinner">
      <mat-spinner [diameter]="diameter()" />
    </div>
  `,
})
export class LoadingSpinnerComponent {
  readonly diameter = input<number>(40);
  readonly overlay  = input<boolean>(false);
}
