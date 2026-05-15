import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { BehaviorSubject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { CompositionService } from '../../../../core/services/composition.service';
import { ProblemListService } from '../../../../core/services/problem-list.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Severity } from '../../../../core/models/problem.model';
import { CompositionResponse } from '../../../../core/models/composition.model';

export interface ProblemFormData {
  ehrId: string;
}

@Component({
  selector: 'app-problem-form',
  templateUrl: './problem-form.component.html'
})
export class ProblemFormComponent implements OnInit {
  form!: FormGroup;
  loading$ = new BehaviorSubject(false);
  readonly severities: Severity[] = ['MILD', 'MODERATE', 'SEVERE'];
  readonly today = new Date();

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ProblemFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProblemFormData,
    private compositionService: CompositionService,
    private problemService: ProblemListService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      icd10Code:   ['', [Validators.required, Validators.pattern(/^[A-Z]\d{2}(\.\d{1,2})?$/)]],
      displayName: ['', [Validators.required, Validators.maxLength(200)]],
      severity:    ['MODERATE', Validators.required],
      status:      ['ACTIVE', Validators.required],
      onsetDate:   [null]
    });
  }

  get icd10Error(): string {
    const ctrl = this.form.get('icd10Code');
    if (ctrl?.hasError('required')) return 'ICD-10 code is required.';
    if (ctrl?.hasError('pattern')) return 'Invalid ICD-10 format. Examples: I10, E11, J45.9';
    return '';
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
        return this.problemService.createProblem(this.data.ehrId, {
          compositionId: comp.id,
          icd10Code:   v.icd10Code,
          displayName: v.displayName,
          severity:    v.severity,
          status:      v.status,
          onsetDate:   v.onsetDate ?? undefined
        });
      })
    ).subscribe({
      next: () => {
        this.loading$.next(false);
        this.notifications.success('Problem added to list.');
        this.dialogRef.close({ refreshProblems: true });
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
