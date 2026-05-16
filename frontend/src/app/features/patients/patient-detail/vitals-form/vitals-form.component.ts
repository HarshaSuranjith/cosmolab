import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { finalize, switchMap } from 'rxjs';

import { VitalSignsService } from '../vital-signs.service';
import { CompositionService } from '../composition.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { LoadingSpinnerComponent } from '../../../../shared/loading-spinner/loading-spinner.component';

export interface VitalsDialogData {
  ehrId: string;
}

@Component({
  selector: 'app-vitals-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatDatepickerModule, MatNativeDateModule,
    LoadingSpinnerComponent,
  ],
  templateUrl: './vitals-form.component.html',
})
export class VitalsFormComponent {
  private readonly fb           = inject(FormBuilder);
  private readonly dialogRef    = inject(MatDialogRef<VitalsFormComponent>);
  private readonly data         = inject<VitalsDialogData>(MAT_DIALOG_DATA);
  private readonly vitalsService = inject(VitalSignsService);
  private readonly compService  = inject(CompositionService);
  private readonly notifications = inject(NotificationService);

  protected readonly loading = signal(false);

  protected readonly form = this.fb.group({
    recordedAt:      [new Date(), Validators.required],
    systolicBp:      [null as number | null, [Validators.min(0), Validators.max(300)]],
    diastolicBp:     [null as number | null, [Validators.min(0), Validators.max(200)]],
    heartRate:       [null as number | null, [Validators.min(0), Validators.max(300)]],
    respiratoryRate: [null as number | null, [Validators.min(0), Validators.max(60)]],
    temperature:     [null as number | null, [Validators.min(30), Validators.max(45)]],
    oxygenSaturation:[null as number | null, [Validators.min(0), Validators.max(100)]],
    weight:          [null as number | null, [Validators.min(0), Validators.max(500)]],
  });

  protected get hasMeasurement(): boolean {
    const v = this.form.value;
    return [v.systolicBp, v.diastolicBp, v.heartRate, v.respiratoryRate,
            v.temperature, v.oxygenSaturation, v.weight].some(x => x !== null && x !== undefined);
  }

  protected submit(): void {
    if (this.form.invalid || !this.hasMeasurement) return;
    this.loading.set(true);

    const now = new Date().toISOString();
    const recordedAt = this.form.value.recordedAt instanceof Date
      ? this.form.value.recordedAt.toISOString()
      : now;

    this.compService.createComposition(this.data.ehrId, {
      type: 'ENCOUNTER_NOTE',
      facilityName: 'Central Hospital',
      startTime: recordedAt,
    }).pipe(
      switchMap(comp => this.vitalsService.createVitals(this.data.ehrId, comp.id, {
        recordedAt,
        ...(this.form.value.systolicBp      !== null && { systolicBp:      this.form.value.systolicBp! }),
        ...(this.form.value.diastolicBp     !== null && { diastolicBp:     this.form.value.diastolicBp! }),
        ...(this.form.value.heartRate       !== null && { heartRate:       this.form.value.heartRate! }),
        ...(this.form.value.respiratoryRate !== null && { respiratoryRate: this.form.value.respiratoryRate! }),
        ...(this.form.value.temperature     !== null && { temperature:     this.form.value.temperature! }),
        ...(this.form.value.oxygenSaturation !== null && { oxygenSaturation: this.form.value.oxygenSaturation! }),
        ...(this.form.value.weight          !== null && { weight:          this.form.value.weight! }),
      })),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: result => {
        this.notifications.success('Vital signs saved');
        this.dialogRef.close({ refreshVitals: true, result });
      },
      error: () => this.notifications.error('Failed to save vital signs'),
    });
  }
}
