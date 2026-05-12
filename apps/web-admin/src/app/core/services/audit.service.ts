import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuditEvent {
  id: string;
  actorUsername: string;
  eventType: string;
  aggregateType: string;
  aggregateId: string;
  detailsJson: string;
  createdAt: string;
}

export interface AuditChainVerification {
  valid: boolean;
  verifiedEvents: number;
  brokenEventId?: string | null;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly apiUrl = `${environment.apiUrl}/audit/events`;

  constructor(private http: HttpClient) {}

  getLatestEvents(limit = 50): Observable<AuditEvent[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<AuditEvent[]>(this.apiUrl, { params });
  }

  verifyChain(limit = 1000): Observable<AuditChainVerification> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<AuditChainVerification>(`${this.apiUrl}/verify`, { params });
  }
}
