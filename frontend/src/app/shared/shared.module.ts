import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { NgxChartsModule } from '@swimlane/ngx-charts';

import { PageHeaderComponent } from './components/page-header/page-header.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { SeverityBadgeComponent } from './components/severity-badge/severity-badge.component';
import { VitalSignsChartComponent } from './components/vital-signs-chart/vital-signs-chart.component';

const MATERIAL_MODULES = [
  MatIconModule,
  MatButtonModule,
  MatProgressSpinnerModule,
  MatChipsModule,
  MatTableModule,
  MatSortModule,
  MatPaginatorModule,
  MatFormFieldModule,
  MatInputModule,
  MatSelectModule,
  MatCardModule,
  MatTabsModule,
  MatDialogModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatTooltipModule,
  MatExpansionModule
];

const SHARED_COMPONENTS = [
  PageHeaderComponent,
  LoadingSpinnerComponent,
  SeverityBadgeComponent,
  VitalSignsChartComponent
];

@NgModule({
  declarations: SHARED_COMPONENTS,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    NgxChartsModule,
    ...MATERIAL_MODULES
  ],
  exports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    NgxChartsModule,
    ...MATERIAL_MODULES,
    ...SHARED_COMPONENTS
  ]
})
export class SharedModule {}
