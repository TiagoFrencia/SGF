import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MatCardModule } from '@angular/material/card';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, TableModule, ButtonModule, InputTextModule, MatCardModule],
  template: `
    <div class="dashboard-container">
      <mat-card class="glass-card p-6">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-slate-800">Catálogo de Productos</h2>
          <button pButton icon="pi pi-plus" label="Nuevo Producto" class="p-button-raised p-button-rounded"></button>
        </div>

        <p-table [value]="products" [paginator]="true" [rows]="10" [responsiveLayout]="'scroll'"
                 class="p-datatable-sm" [globalFilterFields]="['commercialName', 'gtin', 'brand']">
          <ng-template pTemplate="caption">
            <div class="flex">
              <span class="p-input-icon-left ml-auto">
                <i class="pi pi-search"></i>
                <input pInputText type="text" (input)="dt.filterGlobal($any($event.target).value, 'contains')" placeholder="Buscar..." />
              </span>
            </div>
          </ng-template>
          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="gtin">GTIN <p-sortIcon field="gtin"></p-sortIcon></th>
              <th pSortableColumn="commercialName">Nombre <p-sortIcon field="commercialName"></p-sortIcon></th>
              <th pSortableColumn="brand">Marca <p-sortIcon field="brand"></p-sortIcon></th>
              <th pSortableColumn="activeIngredient">Principio Activo <p-sortIcon field="activeIngredient"></p-sortIcon></th>
              <th pSortableColumn="price">Precio <p-sortIcon field="price"></p-sortIcon></th>
              <th>Acciones</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-product>
            <tr>
              <td>{{product.gtin}}</td>
              <td><span class="font-bold">{{product.commercialName}}</span></td>
              <td>{{product.brand}}</td>
              <td>{{product.activeIngredient}}</td>
              <td><span class="text-green-600 font-bold">${{product.price | number:'1.2-2'}}</span></td>
              <td>
                <button pButton icon="pi pi-pencil" class="p-button-text p-button-sm"></button>
                <button pButton icon="pi pi-trash" class="p-button-text p-button-danger p-button-sm"></button>
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="6" class="text-center p-4">No se encontraron productos.</td>
            </tr>
          </ng-template>
        </p-table>
      </mat-card>
    </div>
  `,
  styles: [`
    :host ::ng-deep {
      .p-datatable {
        background: transparent !important;
        .p-datatable-thead > tr > th {
          background: rgba(255, 255, 255, 0.5);
          color: #1e293b;
        }
        .p-datatable-tbody > tr {
          background: transparent;
          &:hover {
            background: rgba(255, 255, 255, 0.3);
          }
        }
      }
    }
    .p-6 { padding: 1.5rem; }
    .flex { display: flex; }
    .justify-between { justify-content: space-between; }
    .items-center { align-items: center; }
    .mb-6 { margin-bottom: 1.5rem; }
    .ml-auto { margin-left: auto; }
    .font-bold { font-weight: 700; }
    .text-slate-800 { color: #1e293b; }
    .text-green-600 { color: #16a34a; }
  `]
})
export class ProductListComponent implements OnInit {
  products: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    // Placeholder logic - would call ApiService in real impl
    this.http.get<any[]>(`${environment.apiUrl}/catalog/products`).subscribe({
      next: (data) => this.products = data,
      error: () => {
        // Mock data if API fails
        this.products = [
          { gtin: '7791234567890', commercialName: 'IBUPROFENO 600MG', brand: 'ACTRON', activeIngredient: 'IBUPROFENO', price: 1250.50 },
          { gtin: '7790987654321', commercialName: 'AMOXICILINA 500MG', brand: 'OPTIAMOX', activeIngredient: 'AMOXICILINA', price: 2100.00 },
          { gtin: '7791112223334', commercialName: 'PARACETAMOL 500MG', brand: 'TAFIROL', activeIngredient: 'PARACETAMOL', price: 850.25 }
        ];
      }
    });
  }
}
