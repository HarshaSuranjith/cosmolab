import {
  ChangeDetectionStrategy, Component, OnInit, inject, signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs';

import { PatientService } from '../patient.service';
import { PatientResponse } from '../../../core/models/patient.model';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { LoadingSpinnerComponent } from '../../../shared/loading-spinner/loading-spinner.component';

const WARDS   = ['ICU', 'CARDIOLOGY', 'ONCOLOGY', 'NEUROLOGY'];
const STATUSES = ['ACTIVE', 'DISCHARGED', 'TRANSFERRED'];

@Component({
  selector: 'app-patient-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatTableModule, MatSortModule, MatPaginatorModule,
    MatInputModule, MatSelectModule, MatFormFieldModule,
    MatButtonModule, MatDialogModule, MatIconModule,
    PageHeaderComponent, LoadingSpinnerComponent,
  ],
  templateUrl: './patient-list.component.html',
  styleUrl: './patient-list.component.scss',
})
export class PatientListComponent implements OnInit {
  private readonly service = inject(PatientService);
  private readonly router  = inject(Router);
  private readonly dialog  = inject(MatDialog);

  protected readonly wards    = WARDS;
  protected readonly statuses = STATUSES;
  protected readonly columns  = ['patient', 'personalNumber', 'ward', 'status', 'dateOfBirth'];

  protected readonly patients      = signal<PatientResponse[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly loading       = signal(false);
  protected readonly error         = signal(false);

  protected readonly searchControl = new FormControl('');
  protected readonly wardControl   = new FormControl('');
  protected readonly statusControl = new FormControl('');

  private currentPage = 0;
  private pageSize    = 20;

  ngOnInit(): void {
    this.load();

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
    ).subscribe(() => { this.currentPage = 0; this.load(); });

    this.wardControl.valueChanges.subscribe(() => { this.currentPage = 0; this.load(); });
    this.statusControl.valueChanges.subscribe(() => { this.currentPage = 0; this.load(); });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.service.getPatients({
      search: this.searchControl.value ?? undefined,
      ward:   this.wardControl.value   ?? undefined,
      status: this.statusControl.value ?? undefined,
      page:   this.currentPage,
      size:   this.pageSize,
    }).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (res: PagedResponse<PatientResponse>) => {
        this.patients.set(res.content);
        this.totalElements.set(res.totalElements);
      },
      error: () => this.error.set(true),
    });
  }

  protected onPage(evt: PageEvent): void {
    this.currentPage = evt.pageIndex;
    this.pageSize    = evt.pageSize;
    this.load();
  }

  protected navigate(id: string): void {
    this.router.navigate(['/patients', id]);
  }

  protected clearSearch(): void {
    this.searchControl.setValue('');
  }

  protected openNewPatientDialog(): void {
    import('@angular/material/dialog').then(({ MatDialog }) => {
      // Sprint 3 stub — coming soon dialog
    });
    this.dialog.open(NewPatientStubDialog);
  }
}

// ─── Inline stub dialog ───────────────────────────────────────────────────────
@Component({
  selector: 'app-new-patient-stub',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>New Patient</h2>
    <mat-dialog-content>
      <p>Patient registration form is coming in a future sprint.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
})
export class NewPatientStubDialog {}
