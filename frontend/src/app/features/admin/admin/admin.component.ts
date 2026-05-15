import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface HealthStatus {
  status: 'UP' | 'DOWN' | 'loading';
  db: 'UP' | 'DOWN' | 'loading';
  broker: 'UP' | 'DOWN' | 'loading';
}

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {
  health: HealthStatus = { status: 'loading', db: 'loading', broker: 'loading' };

  readonly links = [
    { title: 'Grafana', subtitle: 'Monitoring', url: 'http://localhost:3000', testid: 'admin-grafana-link', icon: 'bar_chart' },
    { title: 'RabbitMQ UI', subtitle: 'Message Broker', url: 'http://localhost:15672', testid: 'admin-rabbitmq-link', icon: 'swap_horiz' },
    { title: 'Actuator', subtitle: 'Health & Info', url: 'http://localhost:8080/actuator', testid: 'admin-actuator-link', icon: 'monitor_heart' }
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadHealth();
  }

  private loadHealth(): void {
    this.http.get<{ status: string; components?: { db?: { status: string }; rabbit?: { status: string } } }>
      ('/api/actuator/health').subscribe({
        next: data => {
          this.health = {
            status:  data.status === 'UP' ? 'UP' : 'DOWN',
            db:      data.components?.['db']?.status === 'UP' ? 'UP' : 'DOWN',
            broker:  data.components?.['rabbit']?.status === 'UP' ? 'UP' : 'DOWN'
          };
        },
        error: () => {
          this.health = { status: 'DOWN', db: 'DOWN', broker: 'DOWN' };
        }
      });
  }

  statusColor(s: string): string {
    if (s === 'UP') return '#2E7D32';
    if (s === 'DOWN') return '#C62828';
    return '#9E9E9E';
  }

  statusIcon(s: string): string {
    if (s === 'UP') return 'check_circle';
    if (s === 'DOWN') return 'error';
    return 'hourglass_empty';
  }
}
