import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import {
  BranchTransfer,
  CreateTransferRequest,
  ExpiryAlert,
  InventoryReceiptRequest,
  InventoryReceiptResponse,
  InventoryService,
  ReorderAlert,
  StockLevel
} from '../../../core/services/inventory.service';
import { Product, ProductService } from '../../../core/services/product.service';

interface BranchOption {
  id: string;
  label: string;
}

type TransferStatusFilter = 'PENDING' | 'IN_TRANSIT' | 'RECEIVED' | 'DISPUTED' | 'CANCELLED' | '';

@Component({
  selector: 'app-stock-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    InputTextModule,
    ButtonModule,
    TableModule,
    DropdownModule
  ],
  templateUrl: './stock-view.component.html',
  styleUrl: './stock-view.component.scss'
})
export class StockViewComponent implements OnInit {
  readonly branches: BranchOption[] = [
    { id: '00000000-0000-0000-0000-000000000101', label: 'Sucursal Central Operativa' },
    { id: '00000000-0000-0000-0000-000000000202', label: 'Sucursal Norte Operativa' }
  ];

  stock: StockLevel[] = [];
  expiring: ExpiryAlert[] = [];
  reorderAlerts: ReorderAlert[] = [];
  transfers: BranchTransfer[] = [];
  products: Product[] = [];
  transferBatches: InventoryReceiptResponse[] = [];
  receiveQuantityByTransfer: Record<string, number> = {};

  successMessage = '';
  errorMessage = '';
  transferMessage = '';

  receipt: InventoryReceiptRequest = {
    productId: '',
    lotNumber: '',
    expiresAt: '',
    quantity: 1,
    unitCost: 0
  };

  transfer: CreateTransferRequest = {
    sourceBranchId: this.branches[0].id,
    destinationBranchId: this.branches[1].id,
    productId: '',
    batchId: '',
    quantity: 1,
    notes: ''
  };

  selectedTransferStatus: TransferStatusFilter = '';
  selectedListMode: 'source' | 'destination' = 'source';
  transferProductFilter = '';

  constructor(
    private inventoryService: InventoryService,
    private productService: ProductService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const panel = params.get('panel');
      const status = params.get('status') as TransferStatusFilter | null;
      const productId = params.get('productId');
      const mode = params.get('mode');

      if (panel === 'expiry') {
        this.selectedTransferStatus = '';
      }
      if (status) {
        this.selectedTransferStatus = status;
      }
      if (productId) {
        this.transferProductFilter = productId;
      }
      if (mode === 'destination' || mode === 'source') {
        this.selectedListMode = mode;
      }
    });
    this.loadProducts();
    this.reload();
    this.reloadTransfers();
  }

  reload(): void {
    this.successMessage = '';
    this.errorMessage = '';

    this.inventoryService.getStockLevels().subscribe({
      next: (stock) => this.stock = stock,
      error: (error) => this.errorMessage = error.userMessage || 'No se pudo cargar el stock.'
    });

    this.inventoryService.getExpiringProducts(365).subscribe({
      next: (alerts) => this.expiring = alerts,
      error: () => this.expiring = []
    });

    this.inventoryService.getReorderAlerts().subscribe({
      next: (alerts) => this.reorderAlerts = alerts,
      error: () => this.reorderAlerts = []
    });
  }

  reloadTransfers(): void {
    const source = this.selectedListMode === 'source' ? this.transfer.sourceBranchId : undefined;
    const destination = this.selectedListMode === 'destination' ? this.transfer.destinationBranchId : undefined;
    const status = this.selectedTransferStatus || undefined;
    this.inventoryService.listTransfers(source, destination, status).subscribe({
      next: (transfers) => {
        this.transfers = transfers;
        this.receiveQuantityByTransfer = transfers.reduce((accumulator, transfer) => {
          accumulator[transfer.id] = transfer.quantity;
          return accumulator;
        }, {} as Record<string, number>);
      },
      error: () => this.transfers = []
    });
  }

  receiveStock(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.inventoryService.registerIncomingStock(this.receipt).subscribe({
      next: () => {
        this.successMessage = 'Ingreso de stock registrado correctamente.';
        this.receipt = {
          productId: '',
          lotNumber: '',
          expiresAt: '',
          quantity: 1,
          unitCost: 0
        };
        this.reload();
      },
      error: (error) => {
        this.errorMessage = error.userMessage || 'No se pudo registrar el ingreso.';
      }
    });
  }

  loadTransferBatches(): void {
    this.transfer.batchId = '';
    this.transferBatches = [];
    if (!this.transfer.productId) {
      return;
    }
    this.inventoryService.getBatchesByProduct(this.transfer.productId).subscribe({
      next: (batches) => this.transferBatches = batches,
      error: () => this.transferBatches = []
    });
  }

  createTransfer(): void {
    this.transferMessage = '';
    this.inventoryService.createTransfer(this.transfer).subscribe({
      next: () => {
        this.transferMessage = 'Transferencia creada correctamente.';
        this.transfer.quantity = 1;
        this.transfer.notes = '';
        this.reloadTransfers();
      },
      error: (error) => {
        this.transferMessage = error.userMessage || 'No se pudo crear la transferencia.';
      }
    });
  }

  shipTransfer(transfer: BranchTransfer): void {
    this.inventoryService.shipTransfer(transfer.id, transfer.sourceBranchId).subscribe({
      next: () => {
        this.transferMessage = 'Transferencia despachada.';
        this.reloadTransfers();
      },
      error: (error) => {
        this.transferMessage = error.userMessage || 'No se pudo despachar la transferencia.';
      }
    });
  }

  receiveTransfer(transfer: BranchTransfer): void {
    const receivedQuantity = this.receiveQuantityByTransfer[transfer.id] ?? transfer.quantity;
    this.inventoryService.receiveTransfer(transfer.id, transfer.destinationBranchId, receivedQuantity).subscribe({
      next: () => {
        this.transferMessage = 'Transferencia recibida.';
        this.reloadTransfers();
      },
      error: (error) => {
        this.transferMessage = error.userMessage || 'No se pudo recibir la transferencia.';
      }
    });
  }

  cancelTransfer(transfer: BranchTransfer): void {
    this.inventoryService.cancelTransfer(transfer.id, transfer.sourceBranchId).subscribe({
      next: () => {
        this.transferMessage = 'Transferencia cancelada.';
        this.reloadTransfers();
      },
      error: (error) => {
        this.transferMessage = error.userMessage || 'No se pudo cancelar la transferencia.';
      }
    });
  }

  productLabel(productId: string): string {
    const product = this.products.find((item) => item.id === productId);
    return product ? `${product.sku} - ${product.commercialName}` : productId;
  }

  branchLabel(branchId: string): string {
    return this.branches.find((branch) => branch.id === branchId)?.label ?? branchId;
  }

  canShip(transfer: BranchTransfer): boolean {
    return transfer.status === 'PENDING';
  }

  canReceive(transfer: BranchTransfer): boolean {
    return transfer.status === 'IN_TRANSIT';
  }

  canCancel(transfer: BranchTransfer): boolean {
    return transfer.status === 'PENDING';
  }

  transferCount(status: TransferStatusFilter): number {
    return this.filteredTransfers.filter((transfer) => transfer.status === status).length;
  }

  get filteredTransfers(): BranchTransfer[] {
    return this.transfers.filter((transfer) => {
      const matchesProduct = !this.transferProductFilter || transfer.productId === this.transferProductFilter;
      const matchesStatus = !this.selectedTransferStatus || transfer.status === this.selectedTransferStatus;
      return matchesProduct && matchesStatus;
    });
  }

  private loadProducts(): void {
    this.productService.getProducts().subscribe({
      next: (products) => this.products = products,
      error: () => this.products = []
    });
  }
}
