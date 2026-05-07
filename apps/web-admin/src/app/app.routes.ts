import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  // Future lazy-loaded routes
  { 
    path: 'products', 
    loadComponent: () => import('./features/products/product-list/product-list.component').then(m => m.ProductListComponent) 
  },
  { 
    path: 'inventory', 
    loadComponent: () => import('./features/inventory/stock-view/stock-view.component').then(m => m.StockViewComponent) 
  },
  { 
    path: 'pos', 
    loadComponent: () => import('./features/pos/pos/pos.component').then(m => m.PosComponent) 
  }
];
