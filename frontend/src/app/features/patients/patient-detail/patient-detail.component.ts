import {
  ChangeDetectionStrategy, Component, OnInit, inject, signal,
} from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { finalize } from 'rxjs';

import { PatientService } from '../patient.service';
import { EhrService } from './ehr.service';
import { VitalSignsService } from './vital-signs.service';
import { CompositionService } from './composition.service';
import { ProblemListService } from './problem-list.service';
import { PatientResponse } from '../../../core/models/patient.model';
import { EhrResponse } from '../../../core/models/ehr.model';
import { VitalSignsResponse } from '../../../core/models/vital-signs.model';
import { ProblemResponse } from '../../../core/models/problem.model';
import { CompositionResponse } from '../../../core/models/composition.model';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { LoadingSpinnerComponent } from '../../../shared/loading-spinner/loading-spinner.component';
import { SeverityBadgeComponent } from '../../../shared/severity-badge/severity-badge.component';
import { VitalSignsChartComponent } from '../../../shared/vital-signs-chart/vital-signs-chart.component';
import { VitalsFormComponent, VitalsDialogData } from './vitals-form/vitals-form.component';
import { ProblemFormComponent, ProblemDialogData } from './problem-form/problem-form.component';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    MatTabsModule, MatCardModule, MatButtonModule, MatChipsModule,
    MatTableModule, MatPaginatorModule, MatSelectModule,
    MatFormFieldModule, MatDialogModule, MatExpansionModule,
    LoadingSpinnerComponent,
    SeverityBadgeComponent, VitalSignsChartComponent,
  ],
  templateUrl: './patient-detail.component.html',
  styleUrl: './patient-detail.component.scss',
})
export class PatientDetailComponent implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly dialog         = inject(MatDialog);
  private readonly patientService = inject(PatientService);
  private readonly ehrService     = inject(EhrService);
  private readonly vitalsService  = inject(VitalSignsService);
  private readonly compService    = inject(CompositionService);
  private readonly problemService = inject(ProblemListService);

  protected readonly loading    = signal(false);
  protected readonly error      = signal(false);
  protected readonly patient    = signal<PatientResponse | null>(null);
  protected readonly ehr        = signal<EhrResponse | null>(null);
  protected readonly vitals     = signal<VitalSignsResponse[]>([]);
  protected readonly problems   = signal<ProblemResponse[]>([]);
  protected readonly compositions         = signal<CompositionResponse[]>([]);
  protected readonly compositionTotal     = signal(0);
  protected readonly problemStatusFilter  = signal('ACTIVE');
  protected readonly compositionPage      = signal(0);
  protected readonly problemColumns       = ['icd10Code', 'displayName', 'severity', 'status', 'onsetDate'];
  protected readonly compositionColumns   = ['type', 'facilityName', 'startTime', 'commitTime', 'status'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadAll(id);
  }

  private loadAll(patientId: string): void {
    this.loading.set(true);
    this.error.set(false);

    this.patientService.getPatient(patientId).subscribe({
      next: patient => {
        this.patient.set(patient);
        this.ehrService.getEhrBySubject(patientId).subscribe({
          next: ehr => {
            this.ehr.set(ehr);
            this.loading.set(false);
            this.loadVitals(ehr.ehrId);
            this.loadProblems(ehr.ehrId);
            this.loadCompositions(ehr.ehrId);
          },
          error: () => { this.loading.set(false); this.error.set(true); },
        });
      },
      error: () => { this.loading.set(false); this.error.set(true); },
    });
  }

  private loadVitals(ehrId: string): void {
    this.vitalsService.getVitals(ehrId).subscribe({
      next: v => this.vitals.set(v),
    });
  }

  protected loadProblems(ehrId: string, status = this.problemStatusFilter()): void {
    const s = status === 'ALL' ? undefined : status;
    this.problemService.getProblems(ehrId, s).subscribe({
      next: p => this.problems.set(p),
    });
  }

  protected loadCompositions(ehrId: string, page = 0): void {
    this.compService.getCompositions(ehrId, { page, size: 10 }).subscribe({
      next: (res: PagedResponse<CompositionResponse>) => {
        this.compositions.set(res.content);
        this.compositionTotal.set(res.totalElements);
      },
    });
  }

  protected goBack(): void {
    this.router.navigate(['/patients']);
  }

  protected openVitalsDialog(): void {
    const ehrId = this.ehr()?.ehrId;
    if (!ehrId) return;
    this.dialog.open(VitalsFormComponent, {
      width: '520px',
      data: { ehrId } satisfies VitalsDialogData,
    }).afterClosed().subscribe((res: { refreshVitals?: boolean } | undefined) => {
      if (res?.refreshVitals) this.loadVitals(ehrId);
    });
  }

  protected openProblemDialog(): void {
    const ehrId = this.ehr()?.ehrId;
    if (!ehrId) return;
    this.dialog.open(ProblemFormComponent, {
      width: '520px',
      data: { ehrId } satisfies ProblemDialogData,
    }).afterClosed().subscribe((res: { refreshProblems?: boolean } | undefined) => {
      if (res?.refreshProblems) this.loadProblems(ehrId);
    });
  }

  protected onProblemStatusChange(status: string): void {
    this.problemStatusFilter.set(status);
    const ehrId = this.ehr()?.ehrId;
    if (ehrId) this.loadProblems(ehrId, status);
  }

  protected onCompositionPage(evt: PageEvent): void {
    this.compositionPage.set(evt.pageIndex);
    const ehrId = this.ehr()?.ehrId;
    if (ehrId) this.loadCompositions(ehrId, evt.pageIndex);
  }

  protected genderLabel(g: string): string {
    if (g === 'MALE') return 'M';
    if (g === 'FEMALE') return 'F';
    return 'O';
  }

  protected initials(): string {
    const p = this.patient();
    if (!p) return '??';
    return p.firstName[0] + p.lastName[0];
  }
}
