import { Component, OnInit } from '@angular/core';

export interface MigrationStatus {
  sourceSystem: string;
  totalRecords: number;
  migratedRecords: number;
  failedRecords: number;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  progress: number;
  lastUpdate?: Date;
}

@Component({
  selector: 'app-migration-dashboard',
  template: `
    <div class="migration-container">
      <h2>🔄 Dashboard de Migración ETL</h2>
      
      <div class="summary-cards">
        <div class="card">
          <h3>Total Sistemas</h3>
          <div class="number">{{ systems.length }}</div>
        </div>
        <div class="card">
          <h3>Registros Migrados</h3>
          <div class="number success">{{ getTotalMigrated() | number }}</div>
        </div>
        <div class="card">
          <h3>Errores</h3>
          <div class="number error">{{ getTotalFailed() | number }}</div>
        </div>
        <div class="card">
          <h3>Progreso Global</h3>
          <div class="number">{{ getGlobalProgress() | number:'1.0-0' }}%</div>
        </div>
      </div>

      <div class="systems-grid">
        <div class="system-card" *ngFor="let system of systems">
          <div class="system-header">
            <h3>{{ system.sourceSystem }}</h3>
            <span class="badge" [class]="'badge-' + system.status.toLowerCase()">
              {{ system.status }}
            </span>
          </div>
          
          <div class="progress-section">
            <div class="progress-info">
              <span>{{ system.migratedRecords }} / {{ system.totalRecords }}</span>
              <span>{{ system.progress }}%</span>
            </div>
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                [style.width.%]="system.progress"
                [class]="'fill-' + system.status.toLowerCase()">
              </div>
            </div>
          </div>

          <div class="stats-row">
            <div class="stat">
              <span class="label">Exitosos:</span>
              <span class="value success">{{ system.migratedRecords - system.failedRecords }}</span>
            </div>
            <div class="stat">
              <span class="label">Fallidos:</span>
              <span class="value error">{{ system.failedRecords }}</span>
            </div>
          </div>

          <div class="actions">
            <button 
              class="btn btn-primary" 
              (click)="startMigration(system)"
              [disabled]="system.status === 'IN_PROGRESS'">
              {{ system.status === 'IN_PROGRESS' ? 'Migrando...' : 'Iniciar Migración' }}
            </button>
            <button 
              class="btn btn-secondary"
              (click)="viewDetails(system)">
              Ver Detalles
            </button>
            <button 
              class="btn btn-danger"
              (click)="rollback(system)"
              [disabled]="system.status !== 'COMPLETED'">
              Rollback
            </button>
          </div>

          <div class="last-update" *ngIf="system.lastUpdate">
            Última actualización: {{ system.lastUpdate | date:'dd/MM/yyyy HH:mm' }}
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .migration-container { padding: 20px; }
    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 20px;
      margin-bottom: 30px;
    }
    .card {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      text-align: center;
    }
    .card h3 {
      margin: 0 0 10px 0;
      color: #666;
      font-size: 14px;
    }
    .number {
      font-size: 32px;
      font-weight: bold;
      color: #2196F3;
    }
    .number.success { color: #4CAF50; }
    .number.error { color: #F44336; }
    .systems-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: 20px;
    }
    .system-card {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .system-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .system-header h3 { margin: 0; }
    .badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: bold;
    }
    .badge-pending { background: #E0E0E0; color: #666; }
    .badge-in_progress { background: #FFF3E0; color: #FF9800; }
    .badge-completed { background: #E8F5E9; color: #4CAF50; }
    .badge-failed { background: #FFEBEE; color: #F44336; }
    .progress-section { margin: 16px 0; }
    .progress-info {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 14px;
    }
    .progress-bar {
      height: 12px;
      background: #E0E0E0;
      border-radius: 6px;
      overflow: hidden;
    }
    .progress-fill {
      height: 100%;
      transition: width 0.3s;
    }
    .fill-pending { background: #9E9E9E; }
    .fill-in_progress { background: #FF9800; }
    .fill-completed { background: #4CAF50; }
    .fill-failed { background: #F44336; }
    .stats-row {
      display: flex;
      justify-content: space-around;
      margin: 16px 0;
      padding: 12px;
      background: #F5F5F5;
      border-radius: 4px;
    }
    .stat { text-align: center; }
    .stat .label { display: block; font-size: 12px; color: #666; }
    .stat .value { font-size: 18px; font-weight: bold; }
    .value.success { color: #4CAF50; }
    .value.error { color: #F44336; }
    .actions {
      display: flex;
      gap: 8px;
      margin-top: 16px;
    }
    .btn {
      flex: 1;
      padding: 8px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
    }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-primary { background: #2196F3; color: white; }
    .btn-secondary { background: #9E9E9E; color: white; }
    .btn-danger { background: #F44336; color: white; }
    .last-update {
      margin-top: 12px;
      font-size: 12px;
      color: #999;
      text-align: center;
    }
  `]
})
export class MigrationDashboardComponent implements OnInit {
  systems: MigrationStatus[] = [];

  ngOnInit(): void {
    this.loadSystems();
  }

  loadSystems(): void {
    // TODO: Conectar con backend para obtener estado real
    this.systems = [
      {
        sourceSystem: 'FarmaWin',
        totalRecords: 15000,
        migratedRecords: 12500,
        failedRecords: 45,
        status: 'IN_PROGRESS',
        progress: 83,
        lastUpdate: new Date()
      },
      {
        sourceSystem: 'Nixfarma',
        totalRecords: 8000,
        migratedRecords: 8000,
        failedRecords: 12,
        status: 'COMPLETED',
        progress: 100,
        lastUpdate: new Date(Date.now() - 86400000)
      },
      {
        sourceSystem: 'Sistema Legacy',
        totalRecords: 5000,
        migratedRecords: 0,
        failedRecords: 0,
        status: 'PENDING',
        progress: 0
      }
    ];
  }

  getTotalMigrated(): number {
    return this.systems.reduce((sum, s) => sum + s.migratedRecords, 0);
  }

  getTotalFailed(): number {
    return this.systems.reduce((sum, s) => sum + s.failedRecords, 0);
  }

  getGlobalProgress(): number {
    const total = this.systems.reduce((sum, s) => sum + s.totalRecords, 0);
    const migrated = this.getTotalMigrated();
    return total > 0 ? (migrated / total) * 100 : 0;
  }

  startMigration(system: MigrationStatus): void {
    console.log('Iniciando migración:', system.sourceSystem);
    // TODO: Implementar llamada al backend
  }

  viewDetails(system: MigrationStatus): void {
    console.log('Ver detalles:', system.sourceSystem);
    // TODO: Navegar a vista de detalles
  }

  rollback(system: MigrationStatus): void {
    if (confirm(`¿Está seguro de realizar rollback de ${system.sourceSystem}?`)) {
      console.log('Rollback:', system.sourceSystem);
      // TODO: Implementar rollback
    }
  }
}
