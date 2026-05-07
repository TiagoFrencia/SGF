import { Component, OnInit } from '@angular/core';
import { SyncService } from '../../core/services/sync.service';

@Component({
  selector: 'app-offline-status',
  template: `
    <div class="offline-indicator" *ngIf="(isOnline$ | async) === false">
      <span class="icon">📡</span>
      <span>Modo Offline - {{ pendingCount }} operaciones pendientes</span>
    </div>
  `,
  styles: [`
    .offline-indicator {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      background: #ff9800;
      color: white;
      padding: 8px 16px;
      text-align: center;
      font-weight: bold;
      z-index: 9999;
      box-shadow: 0 2px 4px rgba(0,0,0,0.2);
    }
  `]
})
export class OfflineStatusComponent implements OnInit {
  isOnline$ = this.syncService.isOnline();
  pendingCount = 0;

  constructor(private syncService: SyncService) {}

  ngOnInit(): void {
    setInterval(() => {
      this.pendingCount = this.syncService.getPendingCount();
    }, 2000);
  }
}
