import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { finalize, switchMap } from 'rxjs';

import { ProblemListService } from '../problem-list.service';
import { CompositionService } from '../composition.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LoadingSpinnerComponent } from '../../../../shared/loading-spinner/loading-spinner.component';
import { SeverityBadgeComponent } from '../../../../shared/severity-badge/severity-badge.component';

export interface ProblemDialogData {
  ehrId: string;
}

@Component({
  selector: 'app-problem-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatDatepickerModule, MatNativeDateModule,
    LoadingSpinnerComponent, SeverityBadgeComponent,
  ],
  templateUrl: './problem-form.component.html',
})
export class ProblemFormComponent {
  private readonly fb              = inject(FormBuilder);
  private readonly dialogRef       = inject(MatDialogRef<ProblemFormComponent>);
  private readonly data            = inject<ProblemDialogData>(MAT_DIALOG_DATA);
  private readonly problemService  = inject(ProblemListService);
  private readonly compService     = inject(CompositionService);
  private readonly notifications   = inject(NotificationService);

  protected readonly loading    = signal(false);
  protected readonly severities = ['MILD', 'MODERATE', 'SEVERE'] as const;
  protected readonly statuses   = ['ACTIVE', 'INACTIVE', 'RESOLVED', 'REFUTED'] as const;
  protected readonly today      = new Date();

  protected readonly form = this.fb.group({
    icd10Code:   ['', [Validators.required, Validators.pattern(/^[A-Z]\d{2}(\.\d{1,2})?$/)]],
    displayName: ['', [Validators.required, Validators.maxLength(200)]],
    severity:    ['MODERATE', Validators.required],
    status:      ['ACTIVE', Validators.required],
    onsetDate:   [null as Date | null],
  });

  protected get icd10Error(): string | null {
    const ctrl = this.form.get('icd10Code');
    if (!ctrl?.errors || !ctrl.touched) return null;
    if (ctrl.errors['required']) return 'ICD-10 code is required.';
    return 'Invalid ICD-10 format. Examples: I10, E11, J45.9';
  }

  protected submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);

    const now = new Date().toISOString();
    const onset = this.form.value.onsetDate instanceof Date
      ? this.form.value.onsetDate.toISOString().split('T')[0]
      : undefined;

    this.compService.createComposition(this.data.ehrId, {
      type: 'ENCOUNTER_NOTE',
      facilityName: 'Central Hospital',
      startTime: now,
    }).pipe(
      switchMap((comp: { id: string }) => this.problemService.createProblem(this.data.ehrId, {
        compositionId: comp.id,
        icd10Code:   this.form.value.icd10Code!,
        displayName: this.form.value.displayName!,
        severity:    this.form.value.severity as 'MILD' | 'MODERATE' | 'SEVERE',
        status:      this.form.value.status as 'ACTIVE' | 'INACTIVE' | 'RESOLVED' | 'REFUTED',
        onsetDate:   onset,
      })),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (result: unknown) => {
        this.notifications.success('Problem added');
        this.dialogRef.close({ refreshProblems: true, result });
      },
      error: () => this.notifications.error('Failed to add problem'),
    });
  }
}
