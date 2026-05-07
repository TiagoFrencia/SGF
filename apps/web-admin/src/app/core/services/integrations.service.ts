import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegulatoryStatus {
  afip: ServiceStatus;
  anmat: ServiceStatus;
  adesfa: ServiceStatus;
}

export interface ServiceStatus {
  online: boolean;
  lastCheck: Date;
  pendingOperations: number;
  lastOperation?: OperationInfo;
}

export interface OperationInfo {
  type: string;
  timestamp: Date;
  success: boolean;
  details?: any;
}

export interface AFIPInvoice {
  id: string;
  invoiceNumber: string;
  CAE: string;
  CAEVto?: Date;
  total: number;
  status: 'AUTHORIZED' | 'REJECTED' | 'PENDING';
  timestamp: Date;
}

export interface ANMATTraceability {
  id: string;
  operationType: 'DISPENSE' | 'RETURN' | 'TRANSFER' | 'DESTROY';
  gtin: string;
  batchNumber: string;
  serialNumber?: string;
  expirationDate: string;
  quantity: number;
  timestamp: Date;
  status: 'SENT' | 'CONFIRMED' | 'REJECTED';
}

export interface InsuranceValidation {
  patientId: string;
  patientName: string;
  insurancePlan: string;
  coverage: number;
  valid: boolean;
  validUntil: Date;
  restrictions?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class IntegrationsService {
  private apiUrl = `${environment.apiUrl}/integrations`;

  constructor(private http: HttpClient) {}

  // General Status
  getRegulatoryStatus(): Observable<RegulatoryStatus> {
    return this.http.get<RegulatoryStatus>(`${this.apiUrl}/status`);
  }

  // AFIP Operations
  getAFIPPendingInvoices(): Observable<AFIPInvoice[]> {
    return this.http.get<AFIPInvoice[]>(`${this.apiUrl}/afip/pending`);
  }

  authorizeInvoice(invoiceData: any): Observable<AFIPInvoice> {
    return this.http.post<AFIPInvoice>(`${this.apiUrl}/afip/authorize`, invoiceData);
  }

  cancelInvoice(invoiceNumber: string, reason: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/afip/cancel`, { invoiceNumber, reason });
  }

  getAFIPInvoiceByCAE(cae: string): Observable<AFIPInvoice> {
    return this.http.get<AFIPInvoice>(`${this.apiUrl}/afip/by-cae/${cae}`);
  }

  reprocessFailedAFIP(): Observable<number> {
    return this.http.post<number>(`${this.apiUrl}/afip/reprocess-failed`, {});
  }

  // ANMAT Operations
  sendANMATDispense(traceabilityData: Partial<ANMATTraceability>): Observable<ANMATTraceability> {
    return this.http.post<ANMATTraceability>(`${this.apiUrl}/anmat/dispense`, traceabilityData);
  }

  sendANMATReturn(traceabilityData: Partial<ANMATTraceability>): Observable<ANMATTraceability> {
    return this.http.post<ANMATTraceability>(`${this.apiUrl}/anmat/return`, traceabilityData);
  }

  getANMATPendingOperations(): Observable<ANMATTraceability[]> {
    return this.http.get<ANMATTraceability[]>(`${this.apiUrl}/anmat/pending`);
  }

  verifyANMATProduct(gtin: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/anmat/verify/${gtin}`);
  }

  reprocessFailedANMAT(): Observable<number> {
    return this.http.post<number>(`${this.apiUrl}/anmat/reprocess-failed`, {});
  }

  // ADESFA / Insurance Operations
  validateInsurance(patientId: string, planId?: string): Observable<InsuranceValidation> {
    const params = planId ? new HttpParams().set('planId', planId) : undefined;
    return this.http.get<InsuranceValidation>(`${this.apiUrl}/adesfa/validate/${patientId}`, { params });
  }

  submitInsuranceClaim(claimData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/adesfa/claim`, claimData);
  }

  getInsuranceCoverage(patientId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/adesfa/coverage/${patientId}`);
  }

  // Batch Operations
  processAllPending(): Observable<{ afip: number; anmat: number; adesfa: number }> {
    return this.http.post<{ afip: number; anmat: number; adesfa: number }>(`${this.apiUrl}/process-all`, {});
  }

  getAuditLog(limit?: number): Observable<any[]> {
    const params = limit ? new HttpParams().set('limit', limit.toString()) : undefined;
    return this.http.get<any[]>(`${this.apiUrl}/audit-log`, { params });
  }
}
