import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `
    <div class="spinner-wrapper" [class.overlay]="overlay" data-testid="loading-spinner">
      <mat-spinner [diameter]="diameter"></mat-spinner>
    </div>
  `,
  styles: [`
    .spinner-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 32px;
    }
    .spinner-wrapper.overlay {
      position: absolute;
      inset: 0;
      background: rgba(255, 255, 255, 0.7);
      z-index: 10;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() diameter = 40;
  @Input() overlay = false;
}
