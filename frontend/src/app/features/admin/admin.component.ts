import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';

interface HealthStatus {
  status: string;
  components?: Record<string, { status: string }>;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatChipsModule, MatButtonModule, PageHeaderComponent],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly health = signal<HealthStatus | null>(null);
  protected readonly healthLoading = signal(true);

  ngOnInit(): void {
    this.http.get<HealthStatus>('/actuator/health').subscribe({
      next: h  => { this.health.set(h);   this.healthLoading.set(false); },
      error: () => { this.healthLoading.set(false); },
    });
  }

  protected statusColor(status: string): string {
    if (status === 'UP') return '#2E7D32';
    if (status === 'DOWN') return '#ba1a1a';
    return '#9E9E9E';
  }
}
