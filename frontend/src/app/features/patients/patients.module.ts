import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { PatientsRoutingModule } from './patients-routing.module';
import { PatientListComponent, NewPatientStubDialogComponent } from './patient-list/patient-list.component';
import { PatientDetailComponent } from './patient-detail/patient-detail.component';
import { VitalsFormComponent } from './patient-detail/vitals-form/vitals-form.component';
import { ProblemFormComponent } from './patient-detail/problem-form/problem-form.component';

@NgModule({
  declarations: [
    PatientListComponent,
    NewPatientStubDialogComponent,
    PatientDetailComponent,
    VitalsFormComponent,
    ProblemFormComponent
  ],
  imports: [SharedModule, PatientsRoutingModule]
})
export class PatientsModule {}
