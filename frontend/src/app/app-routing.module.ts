import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: 'ward/ICU', pathMatch: 'full' },
  {
    path: 'ward/:wardId',
    loadChildren: () => import('./features/ward/ward.module').then(m => m.WardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'patients',
    loadChildren: () => import('./features/patients/patients.module').then(m => m.PatientsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: 'ward/ICU' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
