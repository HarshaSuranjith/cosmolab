import {
  ChangeDetectionStrategy, Component, OnInit, computed, inject, signal,
} from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { finalize } from 'rxjs';
import { map } from 'rxjs/operators';

import { WardOverviewService } from '../ward-overview.service';
import { WardOverviewResponse, PatientSummary } from '../../../core/models/ward-overview.model';
import { LoadingSpinnerComponent } from '../../../shared/loading-spinner/loading-spinner.component';

const WARDS = ['ICU', 'CARDIOLOGY', 'ONCOLOGY', 'NEUROLOGY'];

@Component({
  selector: 'app-ward-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatTableModule, MatSortModule, MatChipsModule,
    MatButtonModule, MatSelectModule, MatFormFieldModule,
    LoadingSpinnerComponent,
  ],
  templateUrl: './ward-overview.component.html',
  styleUrl: './ward-overview.component.scss',
})
export class WardOverviewComponent implements OnInit {
  private readonly service = inject(WardOverviewService);
  private readonly router  = inject(Router);
  private readonly route   = inject(ActivatedRoute);

  protected readonly wards = WARDS;
  protected readonly columns = ['index', 'patient', 'status', 'bp', 'hr', 'temp', 'spo2', 'problems'];

  protected readonly overview  = signal<WardOverviewResponse | null>(null);
  protected readonly loading   = signal(false);
  protected readonly error     = signal(false);

  protected readonly currentWard = toSignal(
    this.route.paramMap.pipe(map(p => p.get('wardId') ?? 'ICU')),
    { initialValue: 'ICU' },
  );

  protected readonly flaggedCount   = computed(() =>
    this.overview()?.patients.filter(p => p.flags.length > 0).length ?? 0);
  protected readonly noVitalsCount  = computed(() =>
    this.overview()?.patients.filter(p => !p.latestVitals).length ?? 0);
  protected readonly activeProblems = computed(() =>
    this.overview()?.patients.reduce((s, p) => s + p.activeProblemCount, 0) ?? 0);

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(p => p.get('wardId') ?? 'ICU'),
    ).subscribe(wardId => this.load(wardId));
  }

  protected load(wardId: string): void {
    this.loading.set(true);
    this.error.set(false);
    this.service.getOverview(wardId).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: data => this.overview.set(data),
      error: ()  => this.error.set(true),
    });
  }

  protected refresh(): void {
    this.load(this.currentWard());
  }

  protected onWardChange(ward: string): void {
    this.router.navigate(['/ward', ward]);
  }

  protected navigate(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }

  protected rowClass(p: PatientSummary): string {
    if (p.flags.some(f => f.includes('HIGH') || f.includes('CRITICAL'))) return 'row-critical';
    if (p.flags.length > 0) return 'row-flagged';
    return '';
  }

  protected bpText(p: PatientSummary): string {
    if (!p.latestVitals || p.latestVitals.systolicBp === null) return '—';
    return `${p.latestVitals.systolicBp}/${p.latestVitals.diastolicBp ?? '?'}`;
  }
}
