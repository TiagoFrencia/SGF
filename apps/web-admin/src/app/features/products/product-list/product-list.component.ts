import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Product, ProductService, CreateProductRequest } from '../../../core/services/product.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    MatCardModule,
    MatCheckboxModule
  ],
  template: `
    <div class="dashboard-container">
      <mat-card class="glass-card p-6 form-card">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold text-slate-800">Catálogo de Productos</h2>
          <button pButton type="button" icon="pi pi-refresh" label="Recargar" class="p-button-outlined" (click)="loadProducts()"></button>
        </div>

        <div class="grid">
          <label>
            GTIN
            <input pInputText [(ngModel)]="draft.gtin" placeholder="07791234567890">
          </label>
          <label>
            SKU
            <input pInputText [(ngModel)]="draft.sku" placeholder="AMOX-500">
          </label>
          <label>
            Nombre comercial
            <input pInputText [(ngModel)]="draft.commercialName" placeholder="Amoxicilina 500mg">
          </label>
          <label>
            Marca
            <input pInputText [(ngModel)]="draft.brand" placeholder="Genfar">
          </label>
          <label>
            Principio activo
            <input pInputText [(ngModel)]="draft.activeIngredient" placeholder="Amoxicilina">
          </label>
          <label>
            Presentación
            <input pInputText [(ngModel)]="draft.presentationDescription" placeholder="Caja x 16 cápsulas">
          </label>
          <label>
            Concentración
            <input pInputText [(ngModel)]="draft.concentration" placeholder="500mg">
          </label>
          <label>
            Forma
            <input pInputText [(ngModel)]="draft.form" placeholder="Cápsula">
          </label>
          <label>
            Unidades por paquete
            <input pInputText type="number" [(ngModel)]="draft.unitsPerPackage">
          </label>
          <label>
            Categoría ANMAT
            <input pInputText [(ngModel)]="draft.anmatCategory" placeholder="ANTIBIOTICO">
          </label>
          <label class="checkbox-row">
            <mat-checkbox [(ngModel)]="draft.prescriptionRequired">Requiere receta</mat-checkbox>
          </label>
          <label class="checkbox-row">
            <mat-checkbox [(ngModel)]="draft.requiresTraceability">Requiere trazabilidad</mat-checkbox>
          </label>
        </div>

        <div class="actions">
          <button pButton type="button" label="Crear producto" (click)="createProduct()" [disabled]="saving"></button>
          <span class="status success" *ngIf="successMessage">{{ successMessage }}</span>
          <span class="status error" *ngIf="errorMessage">{{ errorMessage }}</span>
        </div>
      </mat-card>

      <mat-card class="glass-card p-6">
        <p-table #dt [value]="products" [paginator]="true" [rows]="10" [responsiveLayout]="'scroll'"
                 class="p-datatable-sm" [globalFilterFields]="['commercialName', 'gtin', 'brand', 'sku']">
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
              <th>GTIN</th>
              <th>SKU</th>
              <th>Nombre</th>
              <th>Marca</th>
              <th>Principio activo</th>
              <th>Receta</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-product>
            <tr>
              <td>{{ product.gtin }}</td>
              <td>{{ product.sku }}</td>
              <td><span class="font-bold">{{ product.commercialName }}</span></td>
              <td>{{ product.brand || '-' }}</td>
              <td>{{ product.activeIngredient || '-' }}</td>
              <td>{{ product.prescriptionRequired ? 'Sí' : 'No' }}</td>
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
    .p-6 { padding: 1.5rem; }
    .flex { display: flex; }
    .justify-between { justify-content: space-between; }
    .items-center { align-items: center; }
    .mb-6 { margin-bottom: 1.5rem; }
    .ml-auto { margin-left: auto; }
    .font-bold { font-weight: 700; }
    .text-slate-800 { color: #1e293b; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }
    label {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-size: 0.9rem;
      color: #334155;
    }
    .checkbox-row {
      justify-content: flex-end;
    }
    .actions {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-top: 1rem;
    }
    .status.success { color: #15803d; }
    .status.error { color: #b91c1c; }
    .form-card {
      margin-bottom: 1.5rem;
    }
  `]
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  saving = false;
  successMessage = '';
  errorMessage = '';

  draft: CreateProductRequest = this.emptyDraft();

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo cargar el catálogo.';
      }
    });
  }

  createProduct(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.saving = true;
    this.productService.createProduct(this.draft).subscribe({
      next: (product) => {
        this.products = [product, ...this.products];
        this.successMessage = `Producto ${product.sku} creado correctamente.`;
        this.draft = this.emptyDraft();
        this.saving = false;
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo crear el producto.';
        this.saving = false;
      }
    });
  }

  private emptyDraft(): CreateProductRequest {
    return {
      gtin: '',
      sku: '',
      commercialName: '',
      brand: '',
      activeIngredient: '',
      prescriptionRequired: false,
      requiresTraceability: false,
      anmatCategory: '',
      presentationDescription: '',
      concentration: '',
      form: '',
      unitsPerPackage: 1
    };
  }
}
