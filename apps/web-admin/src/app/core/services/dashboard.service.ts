import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardMetrics {
  totalSales: number;
  totalRevenue: number;
  ticketPromedio: number;
  productsSold: number;
  lowStockProducts: number;
  expiringProducts: number;
  pendingPrescriptions: number;
  afipPendingInvoices: number;
}

export interface SalesTrend {
  date: string;
  sales: number;
  revenue: number;
}

export interface TopProduct {
  productId: string;
  productName: string;
  gtin: string;
  quantitySold: number;
  revenue: number;
}

export interface CategoryPerformance {
  category: string;
  sales: number;
  revenue: number;
  margin: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getMetrics(dateFrom?: string, dateTo?: string): Observable<DashboardMetrics> {
    let params = new HttpParams();
    if (dateFrom) params = params.set('from', dateFrom);
    if (dateTo) params = params.set('to', dateTo);
    return this.http.get<DashboardMetrics>(this.apiUrl, { params });
  }

  getSalesTrend(days: number = 30): Observable<SalesTrend[]> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<SalesTrend[]>(`${this.apiUrl}/trends/sales`, { params });
  }

  getTopProducts(limit: number = 10, dateFrom?: string, dateTo?: string): Observable<TopProduct[]> {
    let params = new HttpParams().set('limit', limit.toString());
    if (dateFrom) params = params.set('from', dateFrom);
    if (dateTo) params = params.set('to', dateTo);
    return this.http.get<TopProduct[]>(`${this.apiUrl}/products/top`, { params });
  }

  getCategoryPerformance(dateFrom?: string, dateTo?: string): Observable<CategoryPerformance[]> {
    let params = new HttpParams();
    if (dateFrom) params = params.set('from', dateFrom);
    if (dateTo) params = params.set('to', dateTo);
    return this.http.get<CategoryPerformance[]>(`${this.apiUrl}/categories`, { params });
  }

  getLowStockAlerts(threshold?: number): Observable<any[]> {
    const params = threshold ? new HttpParams().set('threshold', threshold.toString()) : undefined;
    return this.http.get<any[]>(`${this.apiUrl}/alerts/low-stock`, { params });
  }

  getExpiringAlerts(days?: number): Observable<any[]> {
    const params = days ? new HttpParams().set('days', days.toString()) : undefined;
    return this.http.get<any[]>(`${this.apiUrl}/alerts/expiring`, { params });
  }

  getPendingPrescriptions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/pending`);
  }

  getAfipStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/afip/status`);
  }

  getAnmatStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/anmat/status`);
  }

  getRecentActivity(limit: number = 20): Observable<any[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<any[]>(`${this.apiUrl}/activity/recent`, { params });
  }

  exportReport(format: 'pdf' | 'excel' | 'csv'): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.apiUrl}/export`, { params, responseType: 'blob' });
  }
}
