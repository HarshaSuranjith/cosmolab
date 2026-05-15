import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { BehaviorSubject } from 'rxjs';
import { switchMap, finalize } from 'rxjs/operators';
import { WardOverviewService } from '../../../core/services/ward-overview.service';
import { PatientSummary, WardOverviewResponse } from '../../../core/models/ward-overview.model';
import { VitalsRangeUtil } from '../../../core/utils/vitals-range.util';

@Component({
  selector: 'app-ward-overview',
  templateUrl: './ward-overview.component.html',
  styleUrls: ['./ward-overview.component.scss']
})
export class WardOverviewComponent implements OnInit {
  @ViewChild(MatSort) sort!: MatSort;

  displayedColumns = ['index', 'patient', 'status', 'bp', 'heartRate', 'temperature', 'spo2', 'problems'];
  dataSource = new MatTableDataSource<PatientSummary>([]);

  loading$ = new BehaviorSubject(false);
  error = false;
  wardId = '';
  overview: WardOverviewResponse | null = null;

  get flaggedCount(): number {
    return this.overview?.patients.filter(p => p.flags.length > 0).length ?? 0;
  }

  get noVitalsCount(): number {
    return this.overview?.patients.filter(p => p.latestVitals === null).length ?? 0;
  }

  get activeProblemsCount(): number {
    return this.overview?.patients.reduce((sum, p) => sum + p.activeProblemCount, 0) ?? 0;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private wardService: WardOverviewService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        this.wardId = params.get('wardId') ?? 'ICU';
        return this.fetch();
      })
    ).subscribe({
      next: data => this.onDataLoaded(data),
      error: () => this.onError()
    });
  }

  refresh(): void {
    this.fetch().subscribe({
      next: data => this.onDataLoaded(data),
      error: () => this.onError()
    });
  }

  private fetch() {
    this.loading$.next(true);
    this.error = false;
    return this.wardService.getOverview(this.wardId).pipe(
      finalize(() => this.loading$.next(false))
    );
  }

  onDataLoaded(data: WardOverviewResponse): void {
    this.overview = data;
    this.dataSource.data = data.patients;
    setTimeout(() => { this.dataSource.sort = this.sort; });
  }

  onError(): void {
    this.error = true;
  }

  navigateToPatient(patient: PatientSummary): void {
    this.router.navigate(['/patients', patient.patientId]);
  }

  isCritical(patient: PatientSummary): boolean {
    return patient.latestVitals !== null && VitalsRangeUtil.isCritical(patient.latestVitals);
  }

  rowClass(patient: PatientSummary): Record<string, boolean> {
    return {
      'row-critical': this.isCritical(patient),
      'row-flagged': !this.isCritical(patient) && patient.flags.length > 0
    };
  }

  formatBp(patient: PatientSummary): string {
    const v = patient.latestVitals;
    if (!v || v.systolicBp === null) return '—';
    return `${v.systolicBp}/${v.diastolicBp ?? '?'}`;
  }

  formatNum(value: number | null | undefined): string {
    return (value !== null && value !== undefined) ? String(value) : '—';
  }

  formatSpo2(value: number | null | undefined): string {
    return (value !== null && value !== undefined) ? `${value}%` : '—';
  }
}
