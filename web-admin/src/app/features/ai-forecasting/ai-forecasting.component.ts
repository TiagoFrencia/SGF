import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

export interface StockForecast {
  productId: string;
  productName: string;
  currentStock: number;
  predictedDemand: number;
  recommendedOrder: number;
  confidence: number;
  trend: 'UP' | 'DOWN' | 'STABLE';
}

@Component({
  selector: 'app-ai-forecasting',
  template: `
    <div class="forecast-container">
      <h2>📊 Predicción de Demanda con IA</h2>
      
      <div class="filters">
        <form [formGroup]="filterForm">
          <select formControlName="category">
            <option value="">Todas las categorías</option>
            <option value="MEDICINES">Medicamentos</option>
            <option value="PERFUMERY">Perfumería</option>
            <option value="DERMOCOSMETICS">Dermocosmética</option>
          </select>
        </form>
      </div>

      <div class="forecast-grid">
        <div class="forecast-card" *ngFor="let forecast of forecasts">
          <h3>{{ forecast.productName }}</h3>
          <div class="metric">
            <span class="label">Stock Actual:</span>
            <span class="value">{{ forecast.currentStock }}</span>
          </div>
          <div class="metric">
            <span class="label">Demanda Predicha (30d):</span>
            <span class="value highlight">{{ forecast.predictedDemand }}</span>
          </div>
          <div class="metric">
            <span class="label">Pedido Recomendado:</span>
            <span class="value order">{{ forecast.recommendedOrder }}</span>
          </div>
          <div class="confidence">
            <span>Confianza del modelo:</span>
            <div class="progress-bar">
              <div class="fill" [style.width.%]="forecast.confidence"></div>
            </div>
            <span>{{ forecast.confidence }}%</span>
          </div>
          <div class="trend" [class]="'trend-' + forecast.trend.toLowerCase()">
            Tendencia: {{ getTrendLabel(forecast.trend) }}
          </div>
          <button class="btn-order" (click)="createOrder(forecast)">
            Generar Pedido
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .forecast-container { padding: 20px; }
    .filters { margin-bottom: 20px; }
    .forecast-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }
    .forecast-card {
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 16px;
      background: white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .metric {
      display: flex;
      justify-content: space-between;
      margin: 8px 0;
    }
    .highlight { color: #2196F3; font-weight: bold; }
    .order { color: #4CAF50; font-weight: bold; }
    .confidence {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 12px 0;
    }
    .progress-bar {
      flex: 1;
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
    }
    .fill {
      height: 100%;
      background: linear-gradient(90deg, #2196F3, #4CAF50);
      transition: width 0.3s;
    }
    .trend {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: bold;
      text-align: center;
    }
    .trend-up { background: #E8F5E9; color: #4CAF50; }
    .trend-down { background: #FFEBEE; color: #F44336; }
    .trend-stable { background: #E3F2FD; color: #2196F3; }
    .btn-order {
      width: 100%;
      padding: 10px;
      background: #2196F3;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      margin-top: 12px;
    }
    .btn-order:hover { background: #1976D2; }
  `]
})
export class AiForecastingComponent implements OnInit {
  filterForm: FormGroup;
  forecasts: StockForecast[] = [];

  constructor(private fb: FormBuilder) {
    this.filterForm = this.fb.group({
      category: ['']
    });
  }

  ngOnInit(): void {
    this.loadForecasts();
    
    this.filterForm.get('category')?.valueChanges.subscribe(() => {
      this.loadForecasts();
    });
  }

  loadForecasts(): void {
    // TODO: Conectar con el servicio de IA backend
    this.forecasts = [
      {
        productId: 'P001',
        productName: 'Paracetamol 500mg x 30 comp',
        currentStock: 45,
        predictedDemand: 120,
        recommendedOrder: 75,
        confidence: 87,
        trend: 'UP'
      },
      {
        productId: 'P002',
        productName: 'Ibuprofeno 400mg x 20 comp',
        currentStock: 80,
        predictedDemand: 65,
        recommendedOrder: 0,
        confidence: 92,
        trend: 'STABLE'
      },
      {
        productId: 'P003',
        productName: 'Amoxicilina 875mg x 14 comp',
        currentStock: 30,
        predictedDemand: 90,
        recommendedOrder: 60,
        confidence: 78,
        trend: 'UP'
      }
    ];
  }

  getTrendLabel(trend: string): string {
    const labels = {
      'UP': '📈 En aumento',
      'DOWN': '📉 En disminución',
      'STABLE': '➡️ Estable'
    };
    return labels[trend as keyof typeof labels];
  }

  createOrder(forecast: StockForecast): void {
    console.log('Creando pedido para:', forecast);
    // TODO: Implementar creación de pedido
  }
}
