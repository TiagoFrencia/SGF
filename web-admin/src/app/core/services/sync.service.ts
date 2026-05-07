import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface PendingOperation {
  id?: string;
  type: 'SALE' | 'INVENTORY_ADJUSTMENT' | 'PRESCRIPTION';
  payload: any;
  timestamp: number;
  synced: boolean;
  retryCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class SyncService {
  private readonly STORAGE_KEY = 'sgf_pending_operations';
  private isOnline$ = new BehaviorSubject<boolean>(navigator.onLine);
  private pendingOperations: PendingOperation[] = [];

  constructor(private http: HttpClient) {
    this.loadPendingOperations();
    
    window.addEventListener('online', () => {
      this.isOnline$.next(true);
      this.syncPending();
    });

    window.addEventListener('offline', () => {
      this.isOnline$.next(false);
    });
  }

  isOnline(): Observable<boolean> {
    return this.isOnline$.asObservable();
  }

  queueOperation(type: PendingOperation['type'], payload: any): void {
    const operation: PendingOperation = {
      type,
      payload,
      timestamp: Date.now(),
      synced: false,
      retryCount: 0
    };

    this.pendingOperations.push(operation);
    this.savePendingOperations();

    if (navigator.onLine) {
      this.syncPending();
    }
  }

  private async syncPending(): Promise<void> {
    const offlineOps = this.pendingOperations.filter(op => !op.synced);
    
    for (const op of offlineOps) {
      try {
        await this.executeOperation(op);
        op.synced = true;
      } catch (error) {
        console.error(`Error syncing operation ${op.type}:`, error);
        op.retryCount++;
        // Reintento exponencial simple
        if (op.retryCount > 5) {
          console.warn(`Operation ${op.id} failed too many times. Manual intervention required.`);
        }
      }
    }
    
    this.savePendingOperations();
  }

  private executeOperation(op: PendingOperation): Promise<any> {
    let endpoint = '';
    switch (op.type) {
      case 'SALE':
        endpoint = `${environment.apiUrl}/pos/sales`;
        break;
      case 'INVENTORY_ADJUSTMENT':
        endpoint = `${environment.apiUrl}/inventory/adjustments`;
        break;
      case 'PRESCRIPTION':
        endpoint = `${environment.apiUrl}/prescriptions`;
        break;
    }

    return this.http.post(endpoint, op.payload).toPromise();
  }

  private loadPendingOperations(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) {
      this.pendingOperations = JSON.parse(stored);
    }
  }

  private savePendingOperations(): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.pendingOperations));
  }

  getPendingCount(): number {
    return this.pendingOperations.filter(op => !op.synced).length;
  }
}
