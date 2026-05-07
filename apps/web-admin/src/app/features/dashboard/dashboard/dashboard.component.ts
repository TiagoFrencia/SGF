import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule],
  template: `
    <div class="dashboard-container">
      <h1 class="text-3xl font-bold mb-8 text-slate-800">Panel de Control - SGF</h1>
      
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <!-- Stats Card 1 -->
        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500 uppercase tracking-wider">Ventas Hoy</p>
                <h2 class="text-2xl font-bold">$125.400</h2>
              </div>
              <div class="icon-container bg-blue-100 text-blue-600">
                <mat-icon>shopping_cart</mat-icon>
              </div>
            </div>
            <div class="mt-4 flex items-center text-green-500 text-sm">
              <mat-icon class="text-sm">trending_up</mat-icon>
              <span class="ml-1">+12% vs ayer</span>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Stats Card 2 -->
        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500 uppercase tracking-wider">Productos Bajos</p>
                <h2 class="text-2xl font-bold text-red-600">14</h2>
              </div>
              <div class="icon-container bg-red-100 text-red-600">
                <mat-icon>warning</mat-icon>
              </div>
            </div>
            <div class="mt-4">
              <button mat-button color="warn" class="text-xs">VER ALERTAS</button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Stats Card 3 -->
        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500 uppercase tracking-wider">Sincronización</p>
                <h2 class="text-2xl font-bold">ONLINE</h2>
              </div>
              <div class="icon-container bg-green-100 text-green-600">
                <mat-icon>cloud_done</mat-icon>
              </div>
            </div>
            <p class="text-xs text-slate-400 mt-4">Última sync: hace 2 min</p>
          </mat-card-content>
        </mat-card>

        <!-- Stats Card 4 -->
        <mat-card class="glass-card stat-card">
          <mat-card-content>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-slate-500 uppercase tracking-wider">Recetas PAMI</p>
                <h2 class="text-2xl font-bold">45</h2>
              </div>
              <div class="icon-container bg-purple-100 text-purple-600">
                <mat-icon>assignment</mat-icon>
              </div>
            </div>
            <div class="mt-4 flex items-center text-blue-500 text-sm">
              <span class="ml-1">8 pendientes de validación</span>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-12">
        <mat-card class="glass-card">
          <mat-card-header>
            <mat-card-title>Actividad Reciente</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="p-4 text-slate-600 text-center italic">
              Cargando eventos de auditoría inmutable...
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card">
          <mat-card-header>
            <mat-card-title>Predicción de Demanda (AI)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="p-4 text-slate-600 text-center italic">
              Conectando con SGF-AI Engine...
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .stat-card {
      transition: transform 0.3s ease;
      &:hover {
        transform: translateY(-5px);
      }
    }
    .icon-container {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .grid { display: grid; }
    .grid-cols-1 { grid-template-columns: repeat(1, minmax(0, 1fr)); }
    @media (min-width: 768px) { .md\:grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (min-width: 1024px) { 
      .lg\:grid-cols-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
      .lg\:grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    .gap-6 { gap: 1.5rem; }
    .gap-8 { gap: 2rem; }
    .mt-12 { margin-top: 3rem; }
    .mb-8 { margin-bottom: 2rem; }
    .flex { display: flex; }
    .items-center { align-items: center; }
    .justify-between { justify-content: space-between; }
    .text-sm { font-size: 0.875rem; }
    .text-2xl { font-size: 1.5rem; }
    .text-3xl { font-size: 1.875rem; }
    .font-bold { font-weight: 700; }
    .text-slate-500 { color: #64748b; }
    .text-slate-800 { color: #1e293b; }
    .bg-blue-100 { background-color: #dbeafe; }
    .text-blue-600 { color: #2563eb; }
    // ... adding more utility-like styles since we aren't using Tailwind by default
  `]
})
export class DashboardComponent {}
