import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatInputModule, MatFormFieldModule, FormsModule, TableModule],
  template: `
    <div class="dashboard-container flex gap-6">
      <!-- Search & Cart -->
      <div class="flex-grow">
        <mat-card class="glass-card mb-6">
          <mat-card-content class="p-4">
            <div class="flex gap-4 items-center">
              <mat-form-field appearance="outline" class="flex-grow">
                <mat-label>Escanear código o buscar producto (F1)</mat-label>
                <input matInput placeholder="GTIN, SKU o Nombre" [(ngModel)]="searchQuery" (keyup.enter)="addToCart()">
                <mat-icon matSuffix>search</mat-icon>
              </mat-form-field>
              <button mat-raised-button color="primary" (click)="addToCart()" class="h-14">AGREGAR</button>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card flex-grow overflow-hidden">
          <p-table [value]="cartItems" class="p-datatable-sm">
            <ng-template pTemplate="header">
              <tr>
                <th>Producto</th>
                <th>Cant.</th>
                <th>Precio</th>
                <th>Subtotal</th>
                <th></th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-item let-index="rowIndex">
              <tr>
                <td>
                  <div class="font-bold">{{item.name}}</div>
                  <div class="text-xs text-slate-500">GTIN: {{item.gtin}}</div>
                </td>
                <td style="width: 100px">
                  <input type="number" [(ngModel)]="item.quantity" class="w-16 p-1 border rounded" (change)="calculateTotal()">
                </td>
                <td>${{item.price | number:'1.2-2'}}</td>
                <td class="font-bold">${{item.price * item.quantity | number:'1.2-2'}}</td>
                <td>
                  <button mat-icon-button color="warn" (click)="removeItem(index)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr>
                <td colspan="5" class="text-center p-8 text-slate-400">
                  El carrito está vacío. Empiece a escanear productos.
                </td>
              </tr>
            </ng-template>
          </p-table>
        </mat-card>
      </div>

      <!-- Checkout Sidebar -->
      <div class="w-96 flex flex-col gap-6">
        <mat-card class="glass-card">
          <mat-card-header>
            <mat-card-title>Resumen de Venta</mat-card-title>
          </mat-card-header>
          <mat-card-content class="p-6">
            <div class="flex justify-between mb-2">
              <span>Subtotal:</span>
              <span>${{subtotal | number:'1.2-2'}}</span>
            </div>
            <div class="flex justify-between mb-4 text-green-600">
              <span>Cobertura Obra Social:</span>
              <span>-${{coverage | number:'1.2-2'}}</span>
            </div>
            <div class="border-t pt-4 flex justify-between items-center">
              <span class="text-xl font-bold">TOTAL:</span>
              <span class="text-3xl font-bold text-blue-700">${{total | number:'1.2-2'}}</span>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="glass-card">
          <mat-card-content class="p-4 flex flex-col gap-3">
            <button mat-raised-button color="accent" class="w-full h-12" (click)="validateInsurance()">
              <mat-icon>verified_user</mat-icon> VALIDAR OBRA SOCIAL (F4)
            </button>
            <button mat-raised-button color="primary" class="w-full h-16 text-lg" [disabled]="cartItems.length === 0">
              <mat-icon>payments</mat-icon> COBRAR (F12)
            </button>
          </mat-card-content>
        </mat-card>

        <div class="shortcuts text-xs text-slate-500 p-2">
          <p>F1: Buscar | F4: Validar | F12: Cobrar | ESC: Limpiar</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .h-14 { height: 3.5rem; }
    .h-12 { height: 3rem; }
    .h-16 { height: 4rem; }
    .w-96 { width: 24rem; }
    .w-16 { width: 4rem; }
    .gap-4 { gap: 1rem; }
    .p-8 { padding: 2rem; }
    .border-t { border-top: 1px solid #e2e8f0; }
    .text-xl { font-size: 1.25rem; }
    .text-lg { font-size: 1.125rem; }
    .text-blue-700 { color: #1d4ed8; }
  `]
})
export class PosComponent {
  searchQuery = '';
  cartItems: any[] = [];
  subtotal = 0;
  coverage = 0;
  total = 0;

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if (event.key === 'F12') {
      event.preventDefault();
      // handle checkout
    }
    if (event.key === 'F4') {
      event.preventDefault();
      this.validateInsurance();
    }
  }

  addToCart() {
    if (!this.searchQuery) return;
    
    // Simulate finding product
    this.cartItems.push({
      gtin: '7791234' + Math.floor(Math.random() * 1000),
      name: 'PRODUCTO SIMULADO ' + (this.cartItems.length + 1),
      quantity: 1,
      price: Math.random() * 1000 + 500
    });
    
    this.searchQuery = '';
    this.calculateTotal();
  }

  removeItem(index: number) {
    this.cartItems.splice(index, 1);
    this.calculateTotal();
  }

  calculateTotal() {
    this.subtotal = this.cartItems.reduce((acc, item) => acc + (item.price * item.quantity), 0);
    this.total = this.subtotal - this.coverage;
  }

  validateInsurance() {
    // Simulate ADESFA validation
    this.coverage = this.subtotal * 0.4;
    this.calculateTotal();
    alert('Validación Exitosa (OSDE 40%)');
  }
}
