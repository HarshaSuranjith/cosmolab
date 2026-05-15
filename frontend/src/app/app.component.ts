import { Component, OnInit, ViewChild } from '@angular/core';
import { MatSidenav } from '@angular/material/sidenav';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  @ViewChild('sidenav') sidenav!: MatSidenav;

  isMobile = false;

  readonly wards = ['ICU', 'CARDIOLOGY', 'ONCOLOGY', 'NEUROLOGY'];

  constructor(
    private breakpointObserver: BreakpointObserver,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.breakpointObserver
      .observe([Breakpoints.XSmall, Breakpoints.Small, '(max-width: 1023px)'])
      .subscribe(result => {
        this.isMobile = result.matches;
        if (this.sidenav) {
          this.isMobile ? this.sidenav.close() : this.sidenav.open();
        }
      });
  }

  onWardChange(wardId: string): void {
    this.router.navigate(['/ward', wardId]);
  }

  toggleSidenav(): void {
    this.sidenav.toggle();
  }
}
