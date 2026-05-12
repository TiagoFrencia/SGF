import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuditEvent, AuditService } from './audit.service';
import { ExpiryAlert, InventoryService, ReorderAlert } from './inventory.service';

export interface IntegrationHealthCard {
  name: string;
  online: boolean;
  message: string;
}

export interface DashboardSummary {
  reorderAlerts: ReorderAlert[];
  expiryAlerts: ExpiryAlert[];
  recentAuditEvents: AuditEvent[];
  integrations: IntegrationHealthCard[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private inventoryService: InventoryService,
    private auditService: AuditService
  ) {}

  getSummary(): Observable<DashboardSummary> {
    return forkJoin({
      reorderAlerts: this.inventoryService.getReorderAlerts().pipe(catchError(() => of([]))),
      expiryAlerts: this.inventoryService.getExpiringProducts(60).pipe(catchError(() => of([]))),
      recentAuditEvents: this.auditService.getLatestEvents(8).pipe(catchError(() => of([]))),
      afip: this.http.get<unknown>(`${this.apiUrl}/afip/invoices/health`).pipe(catchError(() => of(null))),
      anmat: this.http.get<unknown>(`${this.apiUrl}/anmat/health`).pipe(catchError(() => of(null))),
      adesfa: this.http.get<unknown>(`${this.apiUrl}/adesfa/health`).pipe(catchError(() => of(null)))
    }).pipe(
      map((result) => ({
        reorderAlerts: result.reorderAlerts,
        expiryAlerts: result.expiryAlerts,
        recentAuditEvents: result.recentAuditEvents,
        integrations: [
          this.mapHealth('AFIP', result.afip),
          this.mapHealth('ANMAT', result.anmat),
          this.mapHealth('ADESFA', result.adesfa)
        ]
      }))
    );
  }

  private mapHealth(name: string, payload: unknown): IntegrationHealthCard {
    if (!payload || typeof payload !== 'object') {
      return { name, online: false, message: 'No disponible' };
    }

    const data = payload as Record<string, unknown>;
    return {
      name,
      online: this.readBoolean(data, ['online', 'healthy', 'available', 'valid']),
      message: this.readString(data, ['message', 'status', 'environment', 'tokenStatus']) ?? 'Sin detalle'
    };
  }

  private readBoolean(source: Record<string, unknown>, keys: string[]): boolean {
    return keys.some((key) => source[key] === true);
  }

  private readString(source: Record<string, unknown>, keys: string[]): string | null {
    for (const key of keys) {
      const value = source[key];
      if (typeof value === 'string' && value.trim().length > 0) {
        return value;
      }
    }
    return null;
  }
}
