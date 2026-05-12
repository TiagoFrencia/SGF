import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { TableModule } from 'primeng/table';
import { ActivatedRoute } from '@angular/router';
import { AuditChainVerification, AuditEvent, AuditService } from '../../../core/services/audit.service';

@Component({
  selector: 'app-audit-view',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule, TableModule],
  template: `
    <div class="audit-container">
      <mat-card class="glass-card control-card">
        <div class="header-row">
          <div>
            <h2>Auditoria operativa</h2>
            <p>Consulta del log inmutable y verificacion de integridad de cadena.</p>
          </div>
          <div class="actions">
            <label>
              Evento
              <input type="text" [(ngModel)]="eventTypeFilter" placeholder="SALE_CREATED">
            </label>
            <label>
              Agregado
              <input type="text" [(ngModel)]="aggregateFilter" placeholder="SALE / TRANSFER">
            </label>
            <label>
              Limite
              <input type="number" [(ngModel)]="limit" min="1" max="200">
            </label>
            <button mat-stroked-button type="button" (click)="reload()">Recargar</button>
            <button mat-raised-button color="primary" type="button" (click)="verify()">Verificar cadena</button>
          </div>
        </div>
        <div class="status success" *ngIf="successMessage">{{ successMessage }}</div>
        <div class="status error" *ngIf="errorMessage">{{ errorMessage }}</div>
      </mat-card>

      <mat-card class="glass-card" *ngIf="verification">
        <mat-card-header>
          <mat-card-title>Estado de la cadena</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="verification-row">
            <span class="pill" [class.online]="verification.valid" [class.offline]="!verification.valid">
              {{ verification.valid ? 'VALIDA' : 'INVALIDA' }}
            </span>
            <span>{{ verification.message }}</span>
          </div>
          <div class="verification-meta">
            Eventos verificados: {{ verification.verifiedEvents }}
            <span *ngIf="verification.brokenEventId">· Evento roto: {{ verification.brokenEventId }}</span>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card class="glass-card">
        <mat-card-header>
          <mat-card-title>Eventos recientes</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p-table [value]="filteredEvents" [rows]="12" [paginator]="true" responsiveLayout="scroll">
            <ng-template pTemplate="header">
              <tr>
                <th>Fecha</th>
                <th>Evento</th>
                <th>Actor</th>
                <th>Agregado</th>
                <th>Detalle</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-event>
              <tr>
                <td>{{ event.createdAt | date:'short' }}</td>
                <td>{{ event.eventType }}</td>
                <td>{{ event.actorUsername }}</td>
                <td>{{ event.aggregateType }} · {{ event.aggregateId }}</td>
                <td><code>{{ event.detailsJson }}</code></td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr>
                <td colspan="5" class="empty-state">No hay eventos para mostrar.</td>
              </tr>
            </ng-template>
          </p-table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .audit-container { display: flex; flex-direction: column; gap: 1rem; }
    .control-card { padding: 1rem; }
    .header-row { display: flex; justify-content: space-between; gap: 1rem; align-items: flex-start; }
    .header-row p { margin: 0.25rem 0 0; color: #64748b; }
    .actions { display: flex; gap: 0.75rem; align-items: end; flex-wrap: wrap; }
    label { display: flex; flex-direction: column; gap: 0.25rem; color: #334155; }
    input { min-width: 100px; }
    .verification-row { display: flex; gap: 1rem; align-items: center; }
    .verification-meta { color: #64748b; margin-top: 0.5rem; }
    .pill { border-radius: 999px; padding: 0.35rem 0.75rem; font-size: 0.75rem; font-weight: 700; }
    .pill.online { background: #dcfce7; color: #166534; }
    .pill.offline { background: #fee2e2; color: #991b1b; }
    .status.success { color: #15803d; }
    .status.error { color: #b91c1c; }
    code { white-space: pre-wrap; word-break: break-word; font-size: 0.8rem; }
    .empty-state { text-align: center; color: #64748b; }
  `]
})
export class AuditViewComponent implements OnInit {
  events: AuditEvent[] = [];
  verification: AuditChainVerification | null = null;
  limit = 50;
  eventTypeFilter = '';
  aggregateFilter = '';
  errorMessage = '';
  successMessage = '';

  constructor(private auditService: AuditService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.eventTypeFilter = params.get('eventType') ?? this.eventTypeFilter;
      this.aggregateFilter = params.get('aggregateType') ?? this.aggregateFilter;
    });
    this.reload();
    this.verify();
  }

  reload(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.auditService.getLatestEvents(this.limit).subscribe({
      next: (events) => {
        this.events = events;
        this.successMessage = 'Eventos actualizados.';
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo cargar la auditoria.';
      }
    });
  }

  verify(): void {
    this.auditService.verifyChain(Math.max(this.limit, 200)).subscribe({
      next: (verification) => {
        this.verification = verification;
      },
      error: () => {
        this.verification = null;
      }
    });
  }

  get filteredEvents(): AuditEvent[] {
    return this.events.filter((event) => {
      const eventTypeMatches = !this.eventTypeFilter || event.eventType.toLowerCase().includes(this.eventTypeFilter.toLowerCase());
      const aggregateMatches = !this.aggregateFilter || event.aggregateType.toLowerCase().includes(this.aggregateFilter.toLowerCase());
      return eventTypeMatches && aggregateMatches;
    });
  }
}
