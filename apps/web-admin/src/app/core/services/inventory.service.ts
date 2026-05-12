import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StockLevel {
  batchId: string;
  productId: string;
  productName: string;
  sku: string;
  lotNumber: string;
  expiresAt: string;
  availableQuantity: number;
}

export interface InventoryReceiptRequest {
  productId: string;
  lotNumber: string;
  expiresAt: string;
  quantity: number;
  unitCost: number;
}

export interface InventoryReceiptResponse {
  batchId: string;
  productId: string;
  lotNumber: string;
  expiresAt: string;
  availableQuantity: number;
  unitCost: number;
}

export interface ExpiryAlert {
  batchId: string;
  productId: string;
  productName: string;
  lotNumber: string;
  expiresAt: string;
  availableQuantity: number;
  daysUntilExpiry: number;
  severity: 'WARNING' | 'ACTION' | 'CRITICAL';
}

export interface ReorderAlert {
  productId: string;
  productName: string;
  gtin: string;
  currentStock: number;
  avgDailyDemand: number;
  reorderPoint: number;
  eoq: number;
  safetyStock: number;
  leadTimeDays: number;
  analysisWindowDays: number;
  totalDemandInWindow: number;
  demandStdDev: number;
  needsReorder: boolean;
  calculatedAt: string;
}

export interface BranchTransfer {
  id: string;
  sourceBranchId: string;
  destinationBranchId: string;
  productId: string;
  productName: string;
  batchId: string;
  lotNumber: string;
  quantity: number;
  notes?: string | null;
  status: 'PENDING' | 'IN_TRANSIT' | 'RECEIVED' | 'DISPUTED' | 'CANCELLED';
  shippedAt?: string | null;
  receivedAt?: string | null;
  receivedQuantity?: number | null;
}

export interface CreateTransferRequest {
  sourceBranchId: string;
  destinationBranchId: string;
  productId: string;
  batchId: string;
  quantity: number;
  notes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly apiUrl = `${environment.apiUrl}/inventory`;

  constructor(private http: HttpClient) {}

  getStockLevels(): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${this.apiUrl}/stock`);
  }

  registerIncomingStock(request: InventoryReceiptRequest): Observable<InventoryReceiptResponse> {
    return this.http.post<InventoryReceiptResponse>(`${this.apiUrl}/receipts`, request);
  }

  getBatchesByProduct(productId: string): Observable<InventoryReceiptResponse[]> {
    return this.http.get<InventoryReceiptResponse[]>(`${this.apiUrl}/products/${productId}/batches`);
  }

  getExpiringProducts(days = 90): Observable<ExpiryAlert[]> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<ExpiryAlert[]>(`${this.apiUrl}/alerts/expiry`, { params });
  }

  getReorderAlerts(): Observable<ReorderAlert[]> {
    return this.http.get<ReorderAlert[]>(`${this.apiUrl}/alerts/reorder`);
  }

  createTransfer(request: CreateTransferRequest): Observable<BranchTransfer> {
    return this.http.post<BranchTransfer>(`${this.apiUrl}/transfers`, request);
  }

  shipTransfer(id: string, branchId: string): Observable<BranchTransfer> {
    return this.http.patch<BranchTransfer>(`${this.apiUrl}/transfers/${id}/ship`, { branchId });
  }

  receiveTransfer(id: string, branchId: string, receivedQuantity?: number): Observable<BranchTransfer> {
    return this.http.patch<BranchTransfer>(`${this.apiUrl}/transfers/${id}/receive`, {
      branchId,
      receivedQuantity
    });
  }

  cancelTransfer(id: string, branchId: string): Observable<BranchTransfer> {
    return this.http.patch<BranchTransfer>(`${this.apiUrl}/transfers/${id}/cancel`, { branchId });
  }

  listTransfers(sourceBranchId?: string, destinationBranchId?: string, status?: string): Observable<BranchTransfer[]> {
    let params = new HttpParams();
    if (sourceBranchId) {
      params = params.set('sourceBranchId', sourceBranchId);
    }
    if (destinationBranchId) {
      params = params.set('destinationBranchId', destinationBranchId);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<BranchTransfer[]>(`${this.apiUrl}/transfers`, { params });
  }
}
