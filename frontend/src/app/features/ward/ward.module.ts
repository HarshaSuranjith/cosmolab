import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { WardRoutingModule } from './ward-routing.module';
import { WardOverviewComponent } from './ward-overview/ward-overview.component';

@NgModule({
  declarations: [WardOverviewComponent],
  imports: [SharedModule, WardRoutingModule]
})
export class WardModule {}
