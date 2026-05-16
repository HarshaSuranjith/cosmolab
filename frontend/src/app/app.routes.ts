import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'ward/ICU', pathMatch: 'full' },
  {
    path: 'ward/:wardId',
    loadComponent: () =>
      import('./features/ward/ward-overview/ward-overview.component')
        .then(m => m.WardOverviewComponent),
    canActivate: [authGuard],
  },
  {
    path: 'patients',
    loadComponent: () =>
      import('./features/patients/patient-list/patient-list.component')
        .then(m => m.PatientListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'patients/:id',
    loadComponent: () =>
      import('./features/patients/patient-detail/patient-detail.component')
        .then(m => m.PatientDetailComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/admin/admin.component')
        .then(m => m.AdminComponent),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'ward/ICU' },
];
