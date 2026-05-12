import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { StockViewComponent } from './stock-view.component';
import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';

describe('StockViewComponent', () => {
  it('renders transfer operations and stock alerts from real services', async () => {
    const inventoryService = jasmine.createSpyObj<InventoryService>('InventoryService', [
      'getStockLevels',
      'getExpiringProducts',
      'getReorderAlerts',
      'listTransfers',
      'registerIncomingStock',
      'getBatchesByProduct',
      'createTransfer',
      'shipTransfer',
      'receiveTransfer',
      'cancelTransfer'
    ]);
    const productService = jasmine.createSpyObj<ProductService>('ProductService', ['getProducts']);

    productService.getProducts.and.returnValue(of([
      {
        id: 'prod-1',
        gtin: '7791234567890',
        sku: 'SKU-1',
        commercialName: 'Ibuprofeno',
        brand: 'SGF',
        activeIngredient: 'Ibuprofeno',
        prescriptionRequired: false,
        requiresTraceability: false,
        anmatCategory: null
      }
    ]));
    inventoryService.getStockLevels.and.returnValue(of([
      {
        batchId: 'batch-1',
        productId: 'prod-1',
        productName: 'Ibuprofeno',
        sku: 'SKU-1',
        lotNumber: 'LOT-1',
        expiresAt: '2026-06-01',
        availableQuantity: 3
      }
    ]));
    inventoryService.getExpiringProducts.and.returnValue(of([]));
    inventoryService.getReorderAlerts.and.returnValue(of([]));
    inventoryService.listTransfers.and.returnValue(of([
      {
        id: 'transfer-1',
        sourceBranchId: '00000000-0000-0000-0000-000000000101',
        destinationBranchId: '00000000-0000-0000-0000-000000000202',
        productId: 'prod-1',
        productName: 'Ibuprofeno',
        batchId: 'batch-1',
        lotNumber: 'LOT-1',
        quantity: 2,
        receivedQuantity: null,
        status: 'PENDING',
        notes: 'Reposicion'
      }
    ]));
    inventoryService.registerIncomingStock.and.returnValue(of({
      batchId: 'batch-1',
      productId: 'prod-1',
      lotNumber: 'LOT-1',
      expiresAt: '2026-06-01',
      availableQuantity: 3,
      unitCost: 100
    }));
    inventoryService.getBatchesByProduct.and.returnValue(of([]));
    inventoryService.createTransfer.and.returnValue(of({
      id: 'transfer-1',
      sourceBranchId: '00000000-0000-0000-0000-000000000101',
      destinationBranchId: '00000000-0000-0000-0000-000000000202',
      productId: 'prod-1',
      productName: 'Ibuprofeno',
      batchId: 'batch-1',
      lotNumber: 'LOT-1',
      quantity: 2,
      receivedQuantity: null,
      status: 'PENDING',
      notes: 'Reposicion'
    }));
    inventoryService.shipTransfer.and.returnValue(of({
      id: 'transfer-1',
      sourceBranchId: '00000000-0000-0000-0000-000000000101',
      destinationBranchId: '00000000-0000-0000-0000-000000000202',
      productId: 'prod-1',
      productName: 'Ibuprofeno',
      batchId: 'batch-1',
      lotNumber: 'LOT-1',
      quantity: 2,
      receivedQuantity: null,
      status: 'IN_TRANSIT',
      notes: 'Reposicion'
    }));
    inventoryService.receiveTransfer.and.returnValue(of({
      id: 'transfer-1',
      sourceBranchId: '00000000-0000-0000-0000-000000000101',
      destinationBranchId: '00000000-0000-0000-0000-000000000202',
      productId: 'prod-1',
      productName: 'Ibuprofeno',
      batchId: 'batch-1',
      lotNumber: 'LOT-1',
      quantity: 2,
      receivedQuantity: 2,
      status: 'RECEIVED',
      notes: 'Reposicion'
    }));
    inventoryService.cancelTransfer.and.returnValue(of({
      id: 'transfer-1',
      sourceBranchId: '00000000-0000-0000-0000-000000000101',
      destinationBranchId: '00000000-0000-0000-0000-000000000202',
      productId: 'prod-1',
      productName: 'Ibuprofeno',
      batchId: 'batch-1',
      lotNumber: 'LOT-1',
      quantity: 2,
      receivedQuantity: null,
      status: 'CANCELLED',
      notes: 'Reposicion'
    }));

    await TestBed.configureTestingModule({
      imports: [StockViewComponent, NoopAnimationsModule],
      providers: [
        { provide: InventoryService, useValue: inventoryService },
        { provide: ProductService, useValue: productService },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(convertToParamMap({})) }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(StockViewComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Transferencias multi-sucursal');
    expect(compiled.textContent).toContain('Ibuprofeno');
    expect(compiled.textContent).toContain('PENDING');
    expect(compiled.textContent).toContain('Disputed');
  });
});
