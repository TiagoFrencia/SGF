import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { AuditViewComponent } from './audit-view.component';
import { AuditService } from '../../../core/services/audit.service';

describe('AuditViewComponent', () => {
  it('renders recent events and verification status', async () => {
    const auditService = jasmine.createSpyObj<AuditService>('AuditService', ['getLatestEvents', 'verifyChain']);
    auditService.getLatestEvents.and.returnValue(of([
      {
        id: 'audit-1',
        actorUsername: 'admin',
        eventType: 'BRANCH_TRANSFER_CREATED',
        aggregateType: 'TRANSFER',
        aggregateId: 'transfer-1',
        detailsJson: '{"qty":2}',
        createdAt: '2026-05-12T10:00:00Z'
      }
    ]));
    auditService.verifyChain.and.returnValue(of({
      valid: true,
      verifiedEvents: 42,
      brokenEventId: null,
      message: 'Cadena integra'
    }));

    await TestBed.configureTestingModule({
      imports: [AuditViewComponent, NoopAnimationsModule],
      providers: [
        { provide: AuditService, useValue: auditService },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(convertToParamMap({ eventType: 'BRANCH_TRANSFER_CREATED' })) }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AuditViewComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Auditoria operativa');
    expect(compiled.textContent).toContain('BRANCH_TRANSFER_CREATED');
    expect(compiled.textContent).toContain('VALIDA');
    expect(compiled.textContent).toContain('Cadena integra');
  });
});
