import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, forkJoin } from 'rxjs';
import { switchMap, finalize } from 'rxjs/operators';
import { PatientService } from '../../../core/services/patient.service';
import { EhrService } from '../../../core/services/ehr.service';
import { VitalSignsService } from '../../../core/services/vital-signs.service';
import { ProblemListService } from '../../../core/services/problem-list.service';
import { CompositionService } from '../../../core/services/composition.service';
import { PatientResponse } from '../../../core/models/patient.model';
import { EhrResponse } from '../../../core/models/ehr.model';
import { VitalSignsResponse } from '../../../core/models/vital-signs.model';
import { ProblemResponse, ProblemStatus } from '../../../core/models/problem.model';
import { CompositionResponse, CompositionType } from '../../../core/models/composition.model';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { VitalsFormComponent, VitalsFormData } from './vitals-form/vitals-form.component';
import { ProblemFormComponent, ProblemFormData } from './problem-form/problem-form.component';

@Component({
  selector: 'app-patient-detail',
  templateUrl: './patient-detail.component.html',
  styleUrls: ['./patient-detail.component.scss']
})
export class PatientDetailComponent implements OnInit {
  patient: PatientResponse | null = null;
  ehr: EhrResponse | null = null;
  vitals: VitalSignsResponse[] = [];
  problems: ProblemResponse[] = [];
  compositions: PagedResponse<CompositionResponse> | null = null;

  loading$ = new BehaviorSubject(false);
  error = false;
  selectedTab = 0;

  // Vitals tab controls
  vitalsRange = '10';
  readonly vitalsRangeOptions = ['10', '30', 'all'];

  // Problems tab controls
  problemStatusFilter: ProblemStatus | '' = 'ACTIVE';
  readonly problemStatuses: Array<ProblemStatus | ''> = ['', 'ACTIVE', 'INACTIVE', 'RESOLVED', 'REFUTED'];

  // Compositions tab controls
  compositionTypeFilter: CompositionType | '' = '';
  compositionsPage = 0;
  compositionsPageSize = 10;

  // Expanded composition row
  expandedCompositionId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private patientService: PatientService,
    private ehrService: EhrService,
    private vitalsService: VitalSignsService,
    private problemService: ProblemListService,
    private compositionService: CompositionService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id') ?? '';
      this.loadPatient(id);
    });

    this.route.queryParamMap.subscribe(qp => {
      const tab = parseInt(qp.get('tab') ?? '0', 10);
      this.selectedTab = isNaN(tab) ? 0 : tab;
    });
  }

  private loadPatient(patientId: string): void {
    this.loading$.next(true);
    this.error = false;

    this.patientService.getPatient(patientId).pipe(
      switchMap(patient => {
        this.patient = patient;
        return this.ehrService.getEhrBySubject(patient.id);
      }),
      switchMap((ehr: EhrResponse) => {
        this.ehr = ehr;
        return forkJoin({
          vitals:       this.vitalsService.getVitals(ehr.ehrId),
          problems:     this.problemService.getProblems(ehr.ehrId, 'ACTIVE'),
          compositions: this.compositionService.getCompositions(ehr.ehrId, { page: 0, size: 10 })
        });
      }),
      finalize(() => this.loading$.next(false))
    ).subscribe({
      next: ({ vitals, problems, compositions }) => {
        this.vitals = vitals ?? [];
        this.problems = problems;
        this.compositions = compositions;
      },
      error: () => { this.error = true; }
    });
  }

  onTabChange(index: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: index },
      queryParamsHandling: 'merge'
    });
  }

  goBack(): void {
    this.router.navigate(['/patients']);
  }

  // ── Vitals tab ──────────────────────────────────────────────────────────

  openAddVitals(): void {
    if (!this.ehr) return;
    const ref = this.dialog.open(VitalsFormComponent, {
      width: '520px',
      data: { ehrId: this.ehr.ehrId } as VitalsFormData
    });
    ref.afterClosed().subscribe((result: { refreshVitals?: boolean } | undefined) => {
      if (result?.refreshVitals && this.ehr) {
        this.vitalsService.getVitals(this.ehr.ehrId).subscribe(v => { this.vitals = v ?? []; });
      }
    });
  }

  // ── Problems tab ─────────────────────────────────────────────────────────

  openAddProblem(): void {
    if (!this.ehr) return;
    const ref = this.dialog.open(ProblemFormComponent, {
      width: '480px',
      data: { ehrId: this.ehr.ehrId } as ProblemFormData
    });
    ref.afterClosed().subscribe((result: { refreshProblems?: boolean } | undefined) => {
      if (result?.refreshProblems && this.ehr) {
        const status = this.problemStatusFilter || undefined;
        this.problemService.getProblems(this.ehr.ehrId, status as ProblemStatus | undefined)
          .subscribe(p => { this.problems = p; });
      }
    });
  }

  onProblemStatusChange(status: ProblemStatus | ''): void {
    if (!this.ehr) return;
    this.problemStatusFilter = status;
    const s = status || undefined;
    this.problemService.getProblems(this.ehr.ehrId, s as ProblemStatus | undefined)
      .subscribe(p => { this.problems = p; });
  }

  isResolved(p: ProblemResponse): boolean {
    return p.status === 'RESOLVED' || p.status === 'REFUTED';
  }

  // ── Compositions tab ─────────────────────────────────────────────────────

  toggleCompositionExpand(id: string): void {
    this.expandedCompositionId = this.expandedCompositionId === id ? null : id;
  }

  onCompositionsPageChange(page: number): void {
    if (!this.ehr) return;
    this.compositionsPage = page;
    this.compositionService.getCompositions(this.ehr.ehrId, {
      type: this.compositionTypeFilter || undefined,
      page,
      size: this.compositionsPageSize
    }).subscribe(c => { this.compositions = c; });
  }

  formatDateTime(iso: string): string {
    return iso ? new Date(iso).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' }) : '—';
  }

  formatDate(iso: string): string {
    return iso ? iso.substring(0, 10) : '—';
  }

  get genderLabel(): string {
    const g = this.patient?.gender;
    if (g === 'MALE') return 'M';
    if (g === 'FEMALE') return 'F';
    return 'OTHER';
  }
}
