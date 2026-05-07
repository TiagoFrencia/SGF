import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-regulatory-status',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="status-container">
      <div 
        *ngFor="let service of services" 
        class="status-item"
        [matTooltip]="getTooltip(service)"
        [class.online]="service.online"
        [class.offline]="!service.online"
      >
        <mat-icon *ngIf="service.online">check_circle</mat-icon>
        <mat-icon *ngIf="!service.online" color="warn">error</mat-icon>
        <span class="service-name">{{ service.name }}</span>
        <span *ngIf="service.pending > 0" class="pending-badge">{{ service.pending }}</span>
      </div>
      
      <button 
        *ngIf="hasPendingOperations" 
        mat-icon-button 
        color="accent"
        (click)="processPending.emit()"
        matTooltip="Procesar operaciones pendientes"
      >
        <mat-icon>sync</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .status-container {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.5rem 1rem;
      background: white;
      border-radius: 0.5rem;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }

    .status-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.875rem;
      font-weight: 500;
    }

    .status-item.online {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .status-item.offline {
      background: #ffebee;
      color: #c62828;
    }

    .service-name {
      font-weight: 600;
    }

    .pending-badge {
      background: #ff9800;
      color: white;
      font-size: 0.75rem;
      padding: 0.125rem 0.5rem;
      border-radius: 9999px;
      font-weight: 700;
    }

    mat-icon {
      font-size: 1.25rem;
      width: 1.25rem;
      height: 1.25rem;
    }
  `]
})
export class RegulatoryStatusComponent {
  @Input() services: Array<{
    name: string;
    online: boolean;
    pending: number;
  }> = [];

  @Output() processPending = new EventEmitter<void>();

  get hasPendingOperations(): boolean {
    return this.services.some(s => s.pending > 0);
  }

  getTooltip(service: any): string {
    const status = service.online ? 'En línea' : 'Sin conexión';
    const pending = service.pending > 0 ? ` - ${service.pending} pendiente(s)` : '';
    return `${service.name}: ${status}${pending}`;
  }
}
