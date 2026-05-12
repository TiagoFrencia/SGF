import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TableModule } from 'primeng/table';
import {
  POSService,
  PosOrder,
  SaleCompletedResponse,
  TerminalRecoveryResponse
} from '../../../core/services/pos.service';
import { Product } from '../../../core/services/product.service';

interface TerminalBranchOption {
  id: string;
  label: string;
}

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    TableModule
  ],
  templateUrl: './pos.component.html',
  styleUrl: './pos.component.scss'
})
export class PosComponent implements OnInit {
  readonly branches: TerminalBranchOption[] = [
    { id: '00000000-0000-0000-0000-000000000101', label: 'Sucursal Central Operativa' },
    { id: '00000000-0000-0000-0000-000000000202', label: 'Sucursal Norte Operativa' }
  ];

  searchQuery = '';
  unitPrice = 0;
  searchResults: Product[] = [];
  currentOrder: PosOrder | null = null;
  terminalOrders: PosOrder[] = [];
  completedSale: SaleCompletedResponse | null = null;
  recoveryState: TerminalRecoveryResponse | null = null;
  errorMessage = '';
  successMessage = '';
  activeTerminalId = '';
  activeBranchId = '';
  readonly receiveQuantityByOrder = new Map<string, number>();

  constructor(private posService: POSService) {}

  ngOnInit(): void {
    this.activeTerminalId = this.posService.defaultTerminalId;
    this.activeBranchId = this.posService.defaultBranchId;
    this.reloadTerminalState();
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (event.key === 'F12') {
      event.preventDefault();
      this.completeOrder();
    }
  }

  searchProducts(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.completedSale = null;
    const query = this.searchQuery.trim();
    if (!query) {
      return;
    }
    this.posService.searchProducts(query).subscribe({
      next: (results) => {
        this.searchResults = results;
        if (results.length === 1) {
          this.addProduct(results[0]);
        } else if (results.length === 0) {
          this.errorMessage = 'No se encontraron productos.';
        }
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo buscar productos.';
      }
    });
  }

  addProduct(product: Product): void {
    this.ensureDraftOrder(() => {
      if (!this.currentOrder) {
        return;
      }
      const requestedPrice = this.unitPrice > 0 ? this.unitPrice : null;
      const addCall = /^\d{8,14}$/.test(this.searchQuery.trim())
        ? this.posService.scanAdd(this.currentOrder.orderId, product.gtin, 1, requestedPrice)
        : this.posService.addItem(this.currentOrder.orderId, product.id, 1, requestedPrice);

      addCall.subscribe({
        next: (order) => {
          this.currentOrder = order;
          this.searchResults = [];
          this.searchQuery = '';
          this.successMessage = product.commercialName + ' agregado a la orden'
            + (requestedPrice ? ' con precio manual.' : ' con precio vigente CNPM si estaba disponible.');
        },
        error: (error) => {
          this.errorMessage = error.userMessage || 'No se pudo agregar el producto.';
        }
      });
    });
  }

  markReady(): void {
    if (!this.currentOrder) {
      return;
    }
    this.posService.markReady(this.currentOrder.orderId).subscribe({
      next: (order) => {
        this.currentOrder = order;
        this.successMessage = 'Orden lista para cobrar.';
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo marcar la orden como ready.';
      }
    });
  }

  completeOrder(): void {
    if (!this.currentOrder) {
      return;
    }
    this.posService.completeOrder(this.currentOrder.orderId, {
      paymentMethod: 'CASH',
      idempotencyKey: 'web-admin-' + this.currentOrder.orderId
    }).subscribe({
      next: (sale) => {
        this.completedSale = sale;
        this.successMessage = 'Venta ' + sale.saleId + ' registrada correctamente.';
        this.reloadTerminalState();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo completar la orden.';
      }
    });
  }

  newTerminalOrder(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.completedSale = null;
    this.posService.createTerminalOrder(this.activeTerminalId, {
      branchId: this.activeBranchId,
      notes: 'web-admin-terminal'
    }).subscribe({
      next: (order) => {
        this.currentOrder = order;
        this.successMessage = 'Nueva orden abierta en ' + this.activeTerminalId + '.';
        this.reloadTerminalOrders();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo abrir una nueva orden.';
      }
    });
  }

  switchOrder(orderId: string): void {
    this.posService.switchTerminalOrder(this.activeTerminalId, orderId).subscribe({
      next: (order) => {
        this.currentOrder = order;
        this.successMessage = 'Orden activa actualizada.';
        this.reloadTerminalOrders();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo cambiar la orden activa.';
      }
    });
  }

  recoverTerminal(): void {
    this.posService.recoverTerminal(this.activeTerminalId, this.activeBranchId).subscribe({
      next: (recovery) => {
        this.recoveryState = recovery;
        this.successMessage = 'Terminal recuperada con ' + recovery.recoveredOrders + ' orden(es).';
        this.reloadTerminalState();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo recuperar la terminal.';
      }
    });
  }

  closeTerminal(): void {
    this.posService.closeTerminal(this.activeTerminalId).subscribe({
      next: () => {
        this.currentOrder = null;
        this.terminalOrders = [];
        this.recoveryState = null;
        this.successMessage = 'Terminal cerrada y liberada.';
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo cerrar la terminal.';
      }
    });
  }

  removeOrder(orderId: string): void {
    this.posService.removeTerminalOrder(this.activeTerminalId, orderId).subscribe({
      next: () => {
        this.successMessage = 'Orden removida de la terminal.';
        if (this.currentOrder?.orderId === orderId) {
          this.currentOrder = null;
        }
        this.reloadTerminalState();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo remover la orden.';
      }
    });
  }

  onTerminalContextChange(): void {
    this.currentOrder = null;
    this.terminalOrders = [];
    this.completedSale = null;
    this.recoveryState = null;
    this.reloadTerminalState();
  }

  isActiveOrder(orderId: string): boolean {
    return this.currentOrder?.orderId === orderId;
  }

  countByStatus(status: PosOrder['status']): number {
    return this.terminalOrders.filter((order) => order.status === status).length;
  }

  ordersByStatus(status: PosOrder['status']): PosOrder[] {
    return this.terminalOrders.filter((order) => order.status === status);
  }

  private ensureDraftOrder(callback: () => void): void {
    if (this.currentOrder) {
      callback();
      return;
    }
    this.posService.createTerminalOrder(this.activeTerminalId, {
      branchId: this.activeBranchId,
      notes: 'web-admin'
    }).subscribe({
      next: (order) => {
        this.currentOrder = order;
        this.reloadTerminalOrders();
        callback();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo crear la orden POS.';
      }
    });
  }

  private reloadTerminalState(): void {
    this.reloadTerminalOrders();
    this.posService.getTerminalActiveOrder(this.activeTerminalId).subscribe({
      next: (order) => {
        this.currentOrder = order;
      },
      error: () => {
        this.currentOrder = null;
      }
    });
  }

  private reloadTerminalOrders(): void {
    this.posService.listTerminalOrders(this.activeTerminalId).subscribe({
      next: (orders) => {
        this.terminalOrders = orders;
      },
      error: () => {
        this.terminalOrders = [];
      }
    });
  }
}
