import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { DashboardService, DashboardSummary } from '../../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule],
  template: `
    <div class="dashboard-container">
      <div class="header-row">
        <div>
          <h1>Panel operativo SGF</h1>
          <p class="subtitle">Resumen real de alertas, auditoria e integraciones sobre el backend actual.</p>
        </div>
        <button mat-stroked-button type="button" (click)="reload()">Recargar</button>
      </div>

      <div class="status success" *ngIf="successMessage">{{ successMessage }}</div>
      <div class="status error" *ngIf="errorMessage">{{ errorMessage }}</div>

      <div class="cards-grid" *ngIf="summary">
        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="metric-title">Productos a reponer</div>
            <div class="metric-value">{{ summary.reorderAlerts.length }}</div>
            <div class="metric-footprint" *ngIf="summary.reorderAlerts[0]">
              {{ summary.reorderAlerts[0].productName }}
            </div>
            <a [routerLink]="['/inventory']" [queryParams]="{ panel: 'reorder' }" class="metric-link">Ver inventario</a>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="metric-title">Lotes proximos a vencer</div>
            <div class="metric-value">{{ summary.expiryAlerts.length }}</div>
            <div class="metric-footprint" *ngIf="summary.expiryAlerts[0]">
              {{ summary.expiryAlerts[0].productName }} · {{ summary.expiryAlerts[0].lotNumber }}
            </div>
            <a [routerLink]="['/inventory']" [queryParams]="{ panel: 'expiry' }" class="metric-link">Ver alertas</a>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="metric-title">Eventos recientes</div>
            <div class="metric-value">{{ summary.recentAuditEvents.length }}</div>
            <div class="metric-footprint" *ngIf="summary.recentAuditEvents[0]">
              {{ summary.recentAuditEvents[0].eventType }}
            </div>
            <a [routerLink]="['/audit']" [queryParams]="{ eventType: summary.recentAuditEvents[0].eventType || '' }" class="metric-link">Abrir auditoria</a>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="metric-title">Integraciones online</div>
            <div class="metric-value">{{ onlineIntegrations() }}/{{ summary.integrations.length }}</div>
            <span class="metric-link plain">Estado en tiempo real</span>
          </mat-card-content>
        </mat-card>
      </div>

      <div class="two-col" *ngIf="summary">
        <mat-card class="glass-card">
          <mat-card-header>
            <mat-card-title>Actividad reciente</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="empty-state" *ngIf="summary.recentAuditEvents.length === 0">No hay eventos recientes.</div>
            <div class="audit-event" *ngFor="let event of summary.recentAuditEvents">
              <div class="event-head">
                <strong>{{ event.eventType }}</strong>
                <span>{{ event.createdAt | date:'short' }}</span>
              </div>
              <div class="event-meta">
                {{ event.actorUsername }} · {{ event.aggregateType }} · {{ event.aggregateId }}
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card">
          <mat-card-header>
            <mat-card-title>Salud de integraciones</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="integration-row" *ngFor="let integration of summary.integrations">
              <div>
                <strong>{{ integration.name }}</strong>
                <div class="event-meta">{{ integration.message }}</div>
              </div>
              <span class="pill" [class.online]="integration.online" [class.offline]="!integration.online">
                {{ integration.online ? 'ONLINE' : 'OFFLINE' }}
              </span>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container { display: flex; flex-direction: column; gap: 1.5rem; }
    .header-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
    .subtitle { margin: 0.25rem 0 0; color: #64748b; }
    .cards-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    .two-col { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1rem; }
    .metric-title { color: #64748b; font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.04em; }
    .metric-value { font-size: 2rem; font-weight: 700; margin: 0.5rem 0; color: #0f172a; }
    .metric-link { color: #2563eb; text-decoration: none; font-weight: 600; }
    .metric-link.plain { color: #475569; }
    .metric-footprint { color: #64748b; min-height: 1.2rem; margin-bottom: 0.4rem; font-size: 0.9rem; }
    .audit-event, .integration-row { padding: 0.75rem 0; border-top: 1px solid #e2e8f0; }
    .audit-event:first-child, .integration-row:first-child { border-top: none; padding-top: 0; }
    .event-head { display: flex; justify-content: space-between; gap: 1rem; }
    .event-meta { color: #64748b; font-size: 0.9rem; margin-top: 0.25rem; }
    .pill { border-radius: 999px; padding: 0.35rem 0.75rem; font-size: 0.75rem; font-weight: 700; }
    .pill.online { background: #dcfce7; color: #166534; }
    .pill.offline { background: #fee2e2; color: #991b1b; }
    .status.success { color: #15803d; }
    .status.error { color: #b91c1c; }
    .empty-state { color: #64748b; font-style: italic; padding: 1rem 0; }
  `]
})
export class DashboardComponent implements OnInit {
  summary: DashboardSummary | null = null;
  errorMessage = '';
  successMessage = '';

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.dashboardService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.successMessage = 'Resumen actualizado.';
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo cargar el dashboard operativo.';
      }
    });
  }

  onlineIntegrations(): number {
    return this.summary?.integrations.filter((integration) => integration.online).length ?? 0;
  }
}
