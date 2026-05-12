import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';
import { AuditViewComponent } from './features/audit/audit-view/audit-view.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'audit', component: AuditViewComponent },
  {
    path: 'products',
    loadComponent: () => import('./features/products/product-list/product-list.component').then((m) => m.ProductListComponent)
  },
  {
    path: 'inventory',
    loadComponent: () => import('./features/inventory/stock-view/stock-view.component').then((m) => m.StockViewComponent)
  },
  {
    path: 'pos',
    loadComponent: () => import('./features/pos/pos/pos.component').then((m) => m.PosComponent)
  }
];
