import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from './product.service';
import { InventoryReceiptResponse } from './inventory.service';

export interface PosOrderItem {
  itemId: string;
  productId: string;
  productName: string;
  gtin?: string | null;
  troquel?: string | null;
  productSource?: string | null;
  productSourceUpdatedAt?: string | null;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  batchId?: string | null;
}

export interface PosOrder {
  orderId: string;
  orderNumber: number;
  status: 'DRAFT' | 'READY' | 'COMPLETED' | 'VOIDED';
  customerName?: string | null;
  customerDocument?: string | null;
  totalAmount: number;
  itemCount: number;
  items: PosOrderItem[];
}

export interface CreateDraftRequest {
  branchId: string;
  customerName?: string;
  customerDocument?: string;
  notes?: string;
}

export interface CompleteOrderRequest {
  paymentMethod: string;
  idempotencyKey: string;
  pamiPrescriptionId?: string;
  pamiBeneficiaryId?: string;
  doctorLicense?: string;
  doctorRegion?: string;
}

export interface SaleCompletedResponse {
  saleId: string;
  idempotencyKey: string;
  status: string;
  totalAmount?: number | null;
  completedAt?: string | null;
  paymentMethod?: string | null;
}

export interface TerminalRecoveryResponse {
  terminalId: string;
  branchId: string;
  recoveredOrders: number;
  activeOrderId?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class POSService {
  private readonly productsUrl = `${environment.apiUrl}/products`;
  private readonly inventoryUrl = `${environment.apiUrl}/inventory`;
  private readonly posOrdersUrl = `${environment.apiUrl}/pos/orders`;

  readonly defaultBranchId = '00000000-0000-0000-0000-000000000101';
  readonly defaultTerminalId = 'POS-TERM-001';

  constructor(private http: HttpClient) {}

  searchProducts(query: string): Observable<Product[]> {
    const trimmed = query.trim();
    let params = new HttpParams();
    if (/^\d{8,14}$/.test(trimmed)) {
      params = params.set('gtin', trimmed);
    } else {
      params = params.set('name', trimmed);
    }
    return this.http.get<Product[]>(this.productsUrl, { params });
  }

  getProductByGtin(gtin: string): Observable<Product> {
    return this.http.get<Product>(`${this.productsUrl}/search/gtin/${gtin}`);
  }

  getAvailableBatches(productId: string): Observable<InventoryReceiptResponse[]> {
    return this.http.get<InventoryReceiptResponse[]>(`${this.inventoryUrl}/products/${productId}/batches`);
  }

  createDraft(request: CreateDraftRequest): Observable<PosOrder> {
    return this.http.post<PosOrder>(this.posOrdersUrl, request);
  }

  addItem(orderId: string, productId: string, quantity: number, unitPrice?: number | null, batchId?: string | null): Observable<PosOrder> {
    return this.http.post<PosOrder>(`${this.posOrdersUrl}/${orderId}/items`, {
      productId,
      quantity,
      unitPrice: unitPrice ?? null,
      batchId: batchId ?? null
    });
  }

  scanAdd(orderId: string, gtin: string, quantity: number, unitPrice?: number | null): Observable<PosOrder> {
    return this.http.post<PosOrder>(`${this.posOrdersUrl}/${orderId}/scan`, {
      gtin,
      quantity,
      unitPrice: unitPrice ?? null
    });
  }

  markReady(orderId: string): Observable<PosOrder> {
    return this.http.patch<PosOrder>(`${this.posOrdersUrl}/${orderId}/ready`, {});
  }

  completeOrder(orderId: string, request: CompleteOrderRequest): Observable<SaleCompletedResponse> {
    return this.http.post<SaleCompletedResponse>(`${this.posOrdersUrl}/${orderId}/complete`, request);
  }

  getOrder(orderId: string): Observable<PosOrder> {
    return this.http.get<PosOrder>(`${this.posOrdersUrl}/${orderId}`);
  }

  listOpenOrders(branchId = this.defaultBranchId, status?: string): Observable<PosOrder[]> {
    let params = new HttpParams().set('branchId', branchId);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PosOrder[]>(this.posOrdersUrl, { params });
  }

  createTerminalOrder(terminalId: string, request: CreateDraftRequest): Observable<PosOrder> {
    return this.http.post<PosOrder>(`${this.posOrdersUrl}/terminals/${terminalId}/new`, request);
  }

  switchTerminalOrder(terminalId: string, orderId: string): Observable<PosOrder> {
    return this.http.patch<PosOrder>(`${this.posOrdersUrl}/terminals/${terminalId}/switch/${orderId}`, {});
  }

  getTerminalActiveOrder(terminalId: string): Observable<PosOrder> {
    return this.http.get<PosOrder>(`${this.posOrdersUrl}/terminals/${terminalId}/active`);
  }

  listTerminalOrders(terminalId: string): Observable<PosOrder[]> {
    return this.http.get<PosOrder[]>(`${this.posOrdersUrl}/terminals/${terminalId}`);
  }

  recoverTerminal(terminalId: string, branchId = this.defaultBranchId): Observable<TerminalRecoveryResponse> {
    const params = new HttpParams().set('branchId', branchId);
    return this.http.post<TerminalRecoveryResponse>(`${this.posOrdersUrl}/terminals/${terminalId}/recover`, {}, { params });
  }

  closeTerminal(terminalId: string): Observable<void> {
    return this.http.delete<void>(`${this.posOrdersUrl}/terminals/${terminalId}`);
  }

  removeTerminalOrder(terminalId: string, orderId: string): Observable<void> {
    return this.http.delete<void>(`${this.posOrdersUrl}/terminals/${terminalId}/orders/${orderId}`);
  }
}
