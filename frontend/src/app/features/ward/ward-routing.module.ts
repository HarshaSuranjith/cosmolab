import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { WardOverviewComponent } from './ward-overview/ward-overview.component';

const routes: Routes = [
  { path: '', component: WardOverviewComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class WardRoutingModule {}
