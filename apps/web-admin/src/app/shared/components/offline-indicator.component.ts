import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-offline-indicator',
  standalone: true,
  imports: [CommonModule, MatProgressBarModule, MatIconModule],
  template: `
    <div *ngIf="!isOnline" class="offline-banner">
      <mat-icon>wifi_off</mat-icon>
      <span>Modo Offline - Los cambios se sincronizarán cuando se restablezca la conexión</span>
      <div *ngIf="pendingChanges > 0" class="pending-count">
        {{ pendingChanges }} pendiente(s)
      </div>
    </div>
  `,
  styles: [`
    .offline-banner {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      background: linear-gradient(90deg, #ff9800, #f57c00);
      color: white;
      padding: 0.75rem 1.5rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      z-index: 9999;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      animation: slideDown 0.3s ease-out;
    }

    @keyframes slideDown {
      from { transform: translateY(-100%); }
      to { transform: translateY(0); }
    }

    .pending-count {
      margin-left: auto;
      background: rgba(255,255,255,0.2);
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-weight: 600;
      font-size: 0.875rem;
    }

    mat-icon {
      font-size: 1.5rem;
      width: 1.5rem;
      height: 1.5rem;
    }
  `]
})
export class OfflineIndicatorComponent {
  @Input() isOnline = true;
  @Input() pendingChanges = 0;
}
