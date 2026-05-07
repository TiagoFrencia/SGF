import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatSidenavModule, MatListModule, MatIconModule, MatToolbarModule, MatButtonModule
  ],
  template: `
    <mat-sidenav-container class="app-container">
      <mat-sidenav mode="side" opened class="sidebar glass-card">
        <div class="logo-container p-6">
          <h2 class="text-2xl font-bold text-blue-700">SGF <span class="text-xs font-normal text-slate-500">v1.0</span></h2>
        </div>
        
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/pos" routerLinkActive="active-link">
            <mat-icon matListItemIcon>point_of_sale</mat-icon>
            <span matListItemTitle>Ventas (POS)</span>
          </a>
          <a mat-list-item routerLink="/products" routerLinkActive="active-link">
            <mat-icon matListItemIcon>inventory_2</mat-icon>
            <span matListItemTitle>Productos</span>
          </a>
          <a mat-list-item routerLink="/inventory" routerLinkActive="active-link">
            <mat-icon matListItemIcon>warehouse</mat-icon>
            <span matListItemTitle>Inventario</span>
          </a>
          <div class="divider"></div>
          <a mat-list-item routerLink="/integrations" routerLinkActive="active-link">
            <mat-icon matListItemIcon>extension</mat-icon>
            <span matListItemTitle>Integraciones</span>
          </a>
          <a mat-list-item routerLink="/audit" routerLinkActive="active-link">
            <mat-icon matListItemIcon>history_edu</mat-icon>
            <span matListItemTitle>Auditoría</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar class="bg-transparent">
          <span class="flex-grow"></span>
          <button mat-icon-button>
            <mat-icon>notifications</mat-icon>
          </button>
          <button mat-icon-button>
            <mat-icon>account_circle</mat-icon>
          </button>
        </mat-toolbar>

        <main>
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .app-container {
      height: 100vh;
      background: #f8fafc;
    }
    .sidebar {
      width: 260px;
      border: none;
      margin: 1rem;
      height: calc(100vh - 2rem);
      border-radius: 16px !important;
    }
    .active-link {
      background: rgba(37, 99, 235, 0.1) !important;
      color: #2563eb !important;
      border-right: 4px solid #2563eb;
    }
    .divider {
      height: 1px;
      background: #e2e8f0;
      margin: 1rem;
    }
    main {
      padding: 1rem;
    }
    .p-6 { padding: 1.5rem; }
    .text-2xl { font-size: 1.5rem; }
    .font-bold { font-weight: 700; }
    .text-blue-700 { color: #1d4ed8; }
    .text-xs { font-size: 0.75rem; }
    .text-slate-500 { color: #64748b; }
  `]
})
export class AppComponent {}
