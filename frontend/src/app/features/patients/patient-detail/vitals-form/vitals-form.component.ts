import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { BehaviorSubject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { CompositionService } from '../../../../core/services/composition.service';
import { VitalSignsService } from '../../../../core/services/vital-signs.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CompositionResponse } from '../../../../core/models/composition.model';

export interface VitalsFormData {
  ehrId: string;
}

function atLeastOneMeasurement(group: AbstractControl): ValidationErrors | null {
  const fields = ['systolicBp', 'diastolicBp', 'heartRate', 'respiratoryRate', 'temperature', 'oxygenSaturation', 'weight'];
  const hasValue = fields.some(f => group.get(f)?.value !== null && group.get(f)?.value !== '');
  return hasValue ? null : { atLeastOne: true };
}

@Component({
  selector: 'app-vitals-form',
  templateUrl: './vitals-form.component.html'
})
export class VitalsFormComponent implements OnInit {
  form!: FormGroup;
  loading$ = new BehaviorSubject(false);

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<VitalsFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VitalsFormData,
    private compositionService: CompositionService,
    private vitalsService: VitalSignsService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      recordedAt:      [new Date().toISOString().substring(0, 16), Validators.required],
      systolicBp:      [null, [Validators.min(0), Validators.max(300)]],
      diastolicBp:     [null, [Validators.min(0), Validators.max(200)]],
      heartRate:       [null, [Validators.min(0), Validators.max(300)]],
      respiratoryRate: [null, [Validators.min(0), Validators.max(60)]],
      temperature:     [null, [Validators.min(30), Validators.max(45)]],
      oxygenSaturation:[null, [Validators.min(0), Validators.max(100)]],
      weight:          [null, [Validators.min(0), Validators.max(500)]]
    }, { validators: atLeastOneMeasurement });
  }

  submit(): void {
    if (this.form.invalid) return;

    const now = new Date().toISOString();
    this.loading$.next(true);

    this.compositionService.createComposition(this.data.ehrId, {
      type: 'ENCOUNTER_NOTE',
      facilityName: 'CosmoLab Ward',
      startTime: now
    }).pipe(
      switchMap((comp: CompositionResponse) => {
        const v = this.form.value;
        return this.vitalsService.createVitals(this.data.ehrId, comp.id, {
          recordedAt: new Date(v.recordedAt).toISOString(),
          systolicBp:       v.systolicBp       ?? undefined,
          diastolicBp:      v.diastolicBp      ?? undefined,
          heartRate:        v.heartRate        ?? undefined,
          respiratoryRate:  v.respiratoryRate  ?? undefined,
          temperature:      v.temperature      ?? undefined,
          oxygenSaturation: v.oxygenSaturation ?? undefined,
          weight:           v.weight           ?? undefined
        });
      })
    ).subscribe({
      next: () => {
        this.loading$.next(false);
        this.notifications.success('Vital signs recorded.');
        this.dialogRef.close({ refreshVitals: true });
      },
      error: () => {
        this.loading$.next(false);
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
