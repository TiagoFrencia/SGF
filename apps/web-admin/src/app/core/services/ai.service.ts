import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AISuggestion {
  type: 'FORECAST' | 'FRAUD_ALERT' | 'ANOMALY' | 'RECOMMENDATION';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  title: string;
  description: string;
  confidence: number;
  data: any;
  timestamp: Date;
  actionTaken?: boolean;
}

export interface SalesForecast {
  productId: string;
  productName: string;
  gtin: string;
  currentStock: number;
  predictedSales7d: number;
  predictedSales30d: number;
  recommendedOrderQuantity: number;
  stockoutRisk: 'LOW' | 'MEDIUM' | 'HIGH';
  confidenceInterval: {
    lower: number;
    upper: number;
  };
}

export interface FraudAlert {
  id: string;
  alertType: 'UNUSUAL_PATTERN' | 'QUANTITY_ANOMALY' | 'PRESCRIPTION_MISMATCH' | 'PRICE_DEVIATION';
  description: string;
  involvedProducts: string[];
  involvedUsers?: string[];
  riskScore: number;
  evidence: any[];
  timestamp: Date;
  status: 'PENDING' | 'INVESTIGATING' | 'RESOLVED' | 'FALSE_POSITIVE';
}

export interface InventoryOptimization {
  productId: string;
  productName: string;
  currentMinStock: number;
  recommendedMinStock: number;
  currentMaxStock: number;
  recommendedMaxStock: number;
  potentialSavings: number;
  reasoning: string;
}

@Injectable({
  providedIn: 'root'
})
export class AIService {
  private apiUrl = `${environment.apiUrl}/ai`;

  constructor(private http: HttpClient) {}

  // General AI Status
  getAISuggestions(limit?: number): Observable<AISuggestion[]> {
    const params = limit ? new HttpParams().set('limit', limit.toString()) : undefined;
    return this.http.get<AISuggestion[]>(`${this.apiUrl}/suggestions`, { params });
  }

  markSuggestionAsActioned(suggestionId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/suggestions/${suggestionId}/action`, {});
  }

  // Forecasting
  getSalesForecasts(category?: string): Observable<SalesForecast[]> {
    const params = category ? new HttpParams().set('category', category) : undefined;
    return this.http.get<SalesForecast[]>(`${this.apiUrl}/forecast/sales`, { params });
  }

  getDemandForecast(productId: string, days: number = 30): Observable<any> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get(`${this.apiUrl}/forecast/demand/${productId}`, { params });
  }

  generatePurchaseRecommendations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/forecast/purchase-recommendations`);
  }

  // Fraud Detection
  getFraudAlerts(status?: string): Observable<FraudAlert[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<FraudAlert[]>(`${this.apiUrl}/fraud/alerts`, { params });
  }

  investigateFraud(alertId: string, notes: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/fraud/${alertId}/investigate`, { notes });
  }

  resolveFraud(alertId: string, resolution: string, isFalsePositive: boolean): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/fraud/${alertId}/resolve`, { resolution, isFalsePositive });
  }

  // Anomaly Detection
  detectAnomalies(dataType: 'SALES' | 'STOCK' | 'USERS', days?: number): Observable<any[]> {
    let params = new HttpParams().set('type', dataType);
    if (days) params = params.set('days', days.toString());
    return this.http.get<any[]>(`${this.apiUrl}/anomalies/detect`, { params });
  }

  getAnomalyHistory(productId?: string): Observable<any[]> {
    const params = productId ? new HttpParams().set('productId', productId) : undefined;
    return this.http.get<any[]>(`${this.apiUrl}/anomalies/history`, { params });
  }

  // Inventory Optimization
  getInventoryOptimizations(): Observable<InventoryOptimization[]> {
    return this.http.get<InventoryOptimization[]>(`${this.apiUrl}/optimization/inventory`);
  }

  applyOptimization(productId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/optimization/${productId}/apply`, {});
  }

  // Model Management
  retrainModels(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/models/retrain`, {});
  }

  getModelStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/models/status`);
  }

  exportPredictions(format: 'csv' | 'json'): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.apiUrl}/predictions/export`, { params, responseType: 'blob' });
  }
}
