import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { DashboardService, DashboardSummary } from '../../../core/services/dashboard.service';

describe('DashboardComponent', () => {
  const summary: DashboardSummary = {
    reorderAlerts: [
      {
        productId: 'prod-1',
        productName: 'Ibuprofeno',
        gtin: '7791234567890',
        currentStock: 0,
        avgDailyDemand: 1.5,
        reorderPoint: 4,
        eoq: 8,
        safetyStock: 2,
        leadTimeDays: 2,
        analysisWindowDays: 30,
        totalDemandInWindow: 45,
        demandStdDev: 1.2,
        needsReorder: true,
        calculatedAt: '2026-05-12T10:00:00Z'
      }
    ],
    expiryAlerts: [
      {
        batchId: 'batch-1',
        productId: 'prod-1',
        productName: 'Ibuprofeno',
        lotNumber: 'LOT-1',
        expiresAt: '2026-06-01',
        availableQuantity: 3,
        daysUntilExpiry: 20,
        severity: 'WARNING'
      }
    ],
    recentAuditEvents: [
      {
        id: 'audit-1',
        actorUsername: 'admin',
        eventType: 'SALE_CREATED',
        aggregateType: 'SALE',
        aggregateId: 'sale-1',
        detailsJson: '{}',
        createdAt: '2026-05-12T10:00:00Z'
      }
    ],
    integrations: [
      { name: 'AFIP', online: true, message: 'OK' },
      { name: 'ANMAT', online: false, message: 'Sandbox caido' }
    ]
  };

  it('renders operational backend data', async () => {
    const dashboardService = jasmine.createSpyObj<DashboardService>('DashboardService', ['getSummary']);
    dashboardService.getSummary.and.returnValue(of(summary));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Panel operativo SGF');
    expect(compiled.textContent).toContain('Productos a reponer');
    expect(compiled.textContent).toContain('SALE_CREATED');
    expect(compiled.textContent).toContain('ONLINE');
    expect(compiled.textContent).toContain('OFFLINE');
  });

  it('surfaces controlled empty/error state when summary fails', async () => {
    const dashboardService = jasmine.createSpyObj<DashboardService>('DashboardService', ['getSummary']);
    dashboardService.getSummary.and.returnValue(throwError(() => ({ userMessage: 'Backend no disponible' })));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Backend no disponible');
  });
});
