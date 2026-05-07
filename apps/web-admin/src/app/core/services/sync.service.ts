import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SyncStatus {
  lastSync?: Date;
  nextSync?: Date;
  pendingChanges: number;
  conflicts: number;
  status: 'SYNCED' | 'SYNCING' | 'CONFLICTS' | 'OFFLINE';
}

export interface OfflineQueueItem {
  id: string;
  type: 'SALE' | 'STOCK_MOVEMENT' | 'PRODUCT_UPDATE' | 'CUSTOMER_UPDATE';
  payload: any;
  timestamp: Date;
  retryCount: number;
  status: 'PENDING' | 'PROCESSING' | 'FAILED' | 'COMPLETED';
  errorMessage?: string;
}

export interface SyncConflict {
  id: string;
  entityType: string;
  entityId: string;
  localVersion: any;
  serverVersion: any;
  conflictType: 'UPDATE_CONFLICT' | 'DELETE_CONFLICT' | 'VERSION_MISMATCH';
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class SyncService {
  private apiUrl = `${environment.apiUrl}/sync`;
  private offlineDbName = 'sgf_offline_db';

  constructor(private http: HttpClient) {}

  // Sync Status
  getSyncStatus(): Observable<SyncStatus> {
    return this.http.get<SyncStatus>(`${this.apiUrl}/status`);
  }

  triggerSync(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/trigger`, {});
  }

  // Offline Queue Management
  getOfflineQueue(): Observable<OfflineQueueItem[]> {
    return this.http.get<OfflineQueueItem[]>(`${this.apiUrl}/queue`);
  }

  addToQueue(item: Partial<OfflineQueueItem>): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/queue`, item);
  }

  processQueue(): Observable<{ success: number; failed: number }> {
    return this.http.post<{ success: number; failed: number }>(`${this.apiUrl}/queue/process`, {});
  }

  clearFailedQueue(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/queue/clear-failed`, {});
  }

  // Conflict Resolution
  getConflicts(): Observable<SyncConflict[]> {
    return this.http.get<SyncConflict[]>(`${this.apiUrl}/conflicts`);
  }

  resolveConflict(conflictId: string, resolution: 'LOCAL' | 'SERVER' | 'MERGE', mergedData?: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/conflicts/${conflictId}/resolve`, {
      resolution,
      mergedData
    });
  }

  resolveAllConflicts(resolution: 'LOCAL' | 'SERVER'): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/conflicts/resolve-all`, { resolution });
  }

  // Offline-First Data Access
  async getOfflineData<T>(entityType: string): Promise<T[]> {
    if (typeof indexedDB === 'undefined') {
      console.warn('IndexedDB not available, using fallback');
      return [];
    }

    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.offlineDbName, 1);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(entityType)) {
          resolve([]);
          return;
        }

        const transaction = db.transaction([entityType], 'readonly');
        const store = transaction.objectStore(entityType);
        const getAllRequest = store.getAll();

        getAllRequest.onsuccess = () => resolve(getAllRequest.result);
        getAllRequest.onerror = () => reject(getAllRequest.error);
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as any).result;
        if (!db.objectStoreNames.contains(entityType)) {
          db.createObjectStore(entityType, { keyPath: 'id' });
        }
      };
    });
  }

  async saveOfflineData<T>(entityType: string, data: T): Promise<void> {
    if (typeof indexedDB === 'undefined') {
      console.warn('IndexedDB not available, queuing for later sync');
      await this.addToQueue({
        type: entityType.toUpperCase() as any,
        payload: data,
        timestamp: new Date(),
        retryCount: 0,
        status: 'PENDING'
      } as Partial<OfflineQueueItem>);
      return;
    }

    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.offlineDbName, 1);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(entityType)) {
          resolve();
          return;
        }

        const transaction = db.transaction([entityType], 'readwrite');
        const store = transaction.objectStore(entityType);
        const putRequest = store.put(data);

        putRequest.onsuccess = () => resolve();
        putRequest.onerror = () => reject(putRequest.error);
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as any).result;
        if (!db.objectStoreNames.contains(entityType)) {
          db.createObjectStore(entityType, { keyPath: 'id' });
        }
      };
    });
  }

  async clearOfflineData(entityType: string): Promise<void> {
    if (typeof indexedDB === 'undefined') {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.offlineDbName, 1);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(entityType)) {
          resolve();
          return;
        }

        const transaction = db.transaction([entityType], 'readwrite');
        const store = transaction.objectStore(entityType);
        const clearRequest = store.clear();

        clearRequest.onsuccess = () => resolve();
        clearRequest.onerror = () => reject(clearRequest.error);
      };
    });
  }

  // Network Detection
  isOnline(): boolean {
    return typeof navigator !== 'undefined' && navigator.onLine;
  }

  onOnlineChange(callback: (isOnline: boolean) => void): () => void {
    const handleOnline = () => callback(true);
    const handleOffline = () => callback(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }
}
