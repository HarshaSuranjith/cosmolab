import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, startWith, finalize } from 'rxjs/operators';
import { PatientService } from '../../../core/services/patient.service';
import { PatientResponse, PatientStatus } from '../../../core/models/patient.model';
import { PagedResponse } from '../../../core/models/paged-response.model';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.scss']
})
export class PatientListComponent implements OnInit, OnDestroy {
  displayedColumns = ['patient', 'personalNumber', 'ward', 'status', 'dateOfBirth'];
  patients: PatientResponse[] = [];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  loading$ = new BehaviorSubject(false);
  error = false;

  searchCtrl = new FormControl('');
  wardCtrl   = new FormControl('');
  statusCtrl = new FormControl('');

  readonly wards = ['ICU', 'CARDIOLOGY', 'ONCOLOGY', 'NEUROLOGY'];
  readonly statuses: PatientStatus[] = ['ACTIVE', 'DISCHARGED', 'TRANSFERRED'];

  private destroy$ = new Subject<void>();

  constructor(
    private patientService: PatientService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.searchCtrl.valueChanges.pipe(startWith(''), debounceTime(300), distinctUntilChanged()),
      this.wardCtrl.valueChanges.pipe(startWith('')),
      this.statusCtrl.valueChanges.pipe(startWith(''))
    ]).pipe(
      takeUntil(this.destroy$),
      switchMap(([search, ward, status]: [string | null, string | null, string | null]) => {
        this.currentPage = 0;
        return this.fetchPage(search ?? '', ward ?? '', status ?? '', 0);
      })
    ).subscribe({
      next: page => this.onPageLoaded(page),
      error: () => { this.error = true; }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchPage(
      this.searchCtrl.value ?? '',
      this.wardCtrl.value ?? '',
      this.statusCtrl.value ?? '',
      this.currentPage
    ).subscribe({
      next: page => this.onPageLoaded(page),
      error: () => { this.error = true; }
    });
  }

  private fetchPage(search: string, ward: string, status: string, page: number) {
    this.loading$.next(true);
    this.error = false;
    return this.patientService.getPatients({
      search: search || undefined,
      ward: ward || undefined,
      status: (status as PatientStatus) || undefined,
      page,
      size: this.pageSize
    }).pipe(finalize(() => this.loading$.next(false)));
  }

  private onPageLoaded(page: PagedResponse<PatientResponse>): void {
    this.patients = page.content;
    this.totalElements = page.totalElements;
  }

  navigateToPatient(patient: PatientResponse): void {
    this.router.navigate(['/patients', patient.id]);
  }

  openNewPatientDialog(): void {
    this.dialog.open(NewPatientStubDialogComponent, { width: '400px' });
  }

  clearSearch(): void {
    this.searchCtrl.setValue('');
  }

  formatDate(isoDate: string): string {
    return isoDate ? isoDate.substring(0, 10) : '—';
  }
}

@Component({
  selector: 'app-new-patient-stub-dialog',
  template: `
    <h2 mat-dialog-title>New Patient</h2>
    <mat-dialog-content>
      <p>Patient registration form is coming in a future sprint.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">Close</button>
    </mat-dialog-actions>
  `
})
export class NewPatientStubDialogComponent {
  constructor(private dialogRef: MatDialogRef<NewPatientStubDialogComponent>) {}
  close(): void { this.dialogRef.close(); }
}
