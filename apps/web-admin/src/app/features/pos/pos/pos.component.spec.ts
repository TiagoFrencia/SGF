import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { PosComponent } from './pos.component';
import { POSService, PosOrder } from '../../../core/services/pos.service';

describe('PosComponent', () => {
  it('handles multi-terminal mode with terminal orders and active switch', async () => {
    const posService = jasmine.createSpyObj<POSService>('POSService', [
      'listTerminalOrders',
      'getTerminalActiveOrder',
      'createTerminalOrder',
      'switchTerminalOrder',
      'recoverTerminal',
      'closeTerminal',
      'removeTerminalOrder',
      'searchProducts',
      'addItem',
      'scanAdd',
      'markReady',
      'completeOrder',
      'getOrder'
    ], {
      defaultBranchId: '00000000-0000-0000-0000-000000000101',
      defaultTerminalId: 'POS-TERM-001'
    });

    const draftOrder: PosOrder = {
      orderId: 'order-1',
      orderNumber: 101,
      status: 'DRAFT',
      customerName: 'Cliente Demo',
      customerDocument: null,
      totalAmount: 0,
      itemCount: 0,
      items: []
    };
    const readyOrder: PosOrder = {
      orderId: 'order-2',
      orderNumber: 102,
      status: 'READY',
      customerName: 'Cliente Mostrador',
      customerDocument: null,
      totalAmount: 1200,
      itemCount: 1,
      items: []
    };

    posService.listTerminalOrders.and.returnValue(of([draftOrder, readyOrder]));
    posService.getTerminalActiveOrder.and.returnValue(of(draftOrder));
    posService.createTerminalOrder.and.returnValue(of(readyOrder));
    posService.switchTerminalOrder.and.returnValue(of(readyOrder));
    posService.recoverTerminal.and.returnValue(of({
      terminalId: 'POS-TERM-001',
      branchId: '00000000-0000-0000-0000-000000000101',
      recoveredOrders: 2,
      activeOrderId: 'order-1'
    }));
    posService.closeTerminal.and.returnValue(of(void 0));
    posService.removeTerminalOrder.and.returnValue(of(void 0));
    posService.searchProducts.and.returnValue(of([]));
    posService.addItem.and.returnValue(of(readyOrder));
    posService.scanAdd.and.returnValue(of(readyOrder));
    posService.markReady.and.returnValue(of(readyOrder));
    posService.completeOrder.and.returnValue(of({
      saleId: 'sale-1',
      idempotencyKey: 'idem-1',
      status: 'COMPLETED'
    }));
    posService.getOrder.and.returnValue(of(readyOrder));

    await TestBed.configureTestingModule({
      imports: [PosComponent, NoopAnimationsModule],
      providers: [{ provide: POSService, useValue: posService }]
    }).compileComponents();

    const fixture = TestBed.createComponent(PosComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.terminalOrders.length).toBe(2);

    component.switchOrder('order-2');
    expect(posService.switchTerminalOrder).toHaveBeenCalledWith('POS-TERM-001', 'order-2');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Terminal POS');
    expect(compiled.textContent).toContain('Cola operativa por terminal');
    expect(compiled.textContent).toContain('Cliente Mostrador');
    expect(compiled.textContent).toContain('Ready');
  });
});
