import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Sale {
  id: string;
  invoiceNumber?: string;
  timestamp: Date;
  total: number;
  subtotal: number;
  tax: number;
  discount: number;
  insuranceCoverage?: number;
  items: SaleItem[];
  paymentMethod: string;
  customerId?: string;
  customerName?: string;
  prescriptionId?: string;
  afipCAE?: string;
  anmatTraceabilityId?: string;
}

export interface SaleItem {
  productId: string;
  productName: string;
  gtin: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  batchNumber?: string;
  expirationDate?: string;
  requiresPrescription: boolean;
}

export interface POSConfig {
  branchId: string;
  branchName: string;
  registerId: string;
  allowNegativeStock: boolean;
  requirePrescriptionValidation: boolean;
  defaultInsurancePlan?: string;
}

export interface CheckoutRequest {
  items: CartItem[];
  paymentMethod: string;
  customerId?: string;
  prescriptionId?: string;
  insurancePlanId?: string;
  observations?: string;
}

export interface CartItem {
  productId: string;
  gtin: string;
  quantity: number;
  batchNumber?: string;
}

export interface CheckoutResponse {
  saleId: string;
  invoiceNumber?: string;
  total: number;
  afipCAE?: string;
  anmatTraceabilityId?: string;
  receipt: any;
}

@Injectable({
  providedIn: 'root'
})
export class POSService {
  private apiUrl = `${environment.apiUrl}/pos`;

  constructor(private http: HttpClient) {}

  getConfig(): Observable<POSConfig> {
    return this.http.get<POSConfig>(`${this.apiUrl}/config`);
  }

  searchProduct(query: string): Observable<any[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<any[]>(`${this.apiUrl}/products/search`, { params });
  }

  getProductByGtin(gtin: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/products/gtin/${gtin}`);
  }

  getAvailableBatches(productId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products/${productId}/batches`);
  }

  validatePrescription(prescriptionId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/prescriptions/validate/${prescriptionId}`);
  }

  validateInsurance(customerId: string, planId?: string): Observable<any> {
    const params = planId ? new HttpParams().set('planId', planId) : undefined;
    return this.http.get<any>(`${this.apiUrl}/insurance/validate/${customerId}`, { params });
  }

  checkout(request: CheckoutRequest): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout`, request);
  }

  cancelSale(saleId: string, reason: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/sales/${saleId}`, {
      body: { reason }
    });
  }

  getSaleById(saleId: string): Observable<Sale> {
    return this.http.get<Sale>(`${this.apiUrl}/sales/${saleId}`);
  }

  getSalesByDate(date: string, branchId?: string): Observable<Sale[]> {
    let params = new HttpParams().set('date', date);
    if (branchId) params = params.set('branchId', branchId);
    return this.http.get<Sale[]>(`${this.apiUrl}/sales/by-date`, { params });
  }

  printReceipt(saleId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/sales/${saleId}/receipt`, { responseType: 'blob' });
  }

  sendEmailReceipt(saleId: string, email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/sales/${saleId}/email`, { email });
  }

  getDailySummary(date?: string): Observable<any> {
    const params = date ? new HttpParams().set('date', date) : undefined;
    return this.http.get<any>(`${this.apiUrl}/summary/daily`, { params });
  }

  offlineSync(offlineSales: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync/offline`, offlineSales);
  }
}
