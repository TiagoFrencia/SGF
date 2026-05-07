import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StockMovement {
  id: string;
  productId: string;
  productName: string;
  gtin: string;
  quantity: number;
  type: 'IN' | 'OUT' | 'ADJUSTMENT' | 'TRANSFER';
  reason?: string;
  batchNumber?: string;
  expirationDate?: string;
  timestamp: Date;
  userId: string;
  userName?: string;
}

export interface StockLevel {
  productId: string;
  productName: string;
  gtin: string;
  totalStock: number;
  availableStock: number;
  reservedStock: number;
  batches: Batch[];
}

export interface Batch {
  batchNumber: string;
  expirationDate: string;
  quantity: number;
  location?: string;
}

export interface StockFilter {
  productId?: string;
  gtin?: string;
  lowStock?: boolean;
  expiringSoon?: boolean;
  days?: number;
  page?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private apiUrl = `${environment.apiUrl}/inventory`;

  constructor(private http: HttpClient) {}

  getStockLevels(filter?: StockFilter): Observable<any> {
    let params = new HttpParams();
    if (filter) {
      if (filter.productId) params = params.set('productId', filter.productId);
      if (filter.gtin) params = params.set('gtin', filter.gtin);
      if (filter.lowStock) params = params.set('lowStock', 'true');
      if (filter.expiringSoon) params = params.set('expiringSoon', 'true');
      if (filter.days) params = params.set('days', filter.days.toString());
      if (filter.page !== undefined) params = params.set('page', filter.page.toString());
      if (filter.size !== undefined) params = params.set('size', filter.size.toString());
    }
    return this.http.get<any>(`${this.apiUrl}/stock`, { params });
  }

  getStockMovements(productId?: string, days?: number): Observable<StockMovement[]> {
    let params = new HttpParams();
    if (productId) params = params.set('productId', productId);
    if (days) params = params.set('days', days.toString());
    return this.http.get<StockMovement[]>(`${this.apiUrl}/movements`, { params });
  }

  registerIncomingStock(movement: Partial<StockMovement>): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.apiUrl}/movements/in`, movement);
  }

  registerOutgoingStock(movement: Partial<StockMovement>): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.apiUrl}/movements/out`, movement);
  }

  adjustStock(movement: Partial<StockMovement>): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.apiUrl}/movements/adjust`, movement);
  }

  getLowStockProducts(threshold?: number): Observable<StockLevel[]> {
    const params = threshold ? `?threshold=${threshold}` : '';
    return this.http.get<StockLevel[]>(`${this.apiUrl}/alerts/low-stock${params}`);
  }

  getExpiringProducts(days: number = 30): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${this.apiUrl}/alerts/expiring?days=${days}`);
  }

  getBatchesByProduct(productId: string): Observable<Batch[]> {
    return this.http.get<Batch[]>(`${this.apiUrl}/products/${productId}/batches`);
  }

  transferStock(fromLocation: string, toLocation: string, items: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/transfer`, { fromLocation, toLocation, items });
  }

  getInventoryValue(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/value`);
  }

  performInventoryCount(countData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/count`, countData);
  }
}
