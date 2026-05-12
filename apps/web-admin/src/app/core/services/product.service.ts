import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  gtin: string;
  sku: string;
  commercialName: string;
  brand?: string | null;
  activeIngredient?: string | null;
  laboratory?: string | null;
  laboratoryCode?: string | null;
  snomedCode?: string | null;
  troquel?: string | null;
  barcode?: string | null;
  source?: string | null;
  sourceUpdatedAt?: string | null;
  latestRetailPrice?: number | null;
  latestPamiAffiliatePrice?: number | null;
  latestPamiDiscountCode?: number | null;
  latestPamiDiscountLabel?: string | null;
  latestPriceSource?: string | null;
  latestPriceEffectiveDate?: string | null;
  prescriptionRequired: boolean;
  requiresTraceability: boolean;
  anmatCategory?: string | null;
  presentationDescription?: string | null;
  concentration?: string | null;
  form?: string | null;
  unitsPerPackage?: number | null;
}

export interface ProductFilter {
  gtin?: string;
  name?: string;
}

export interface CreateProductRequest {
  gtin: string;
  sku: string;
  commercialName: string;
  brand?: string;
  activeIngredient?: string;
  prescriptionRequired: boolean;
  requiresTraceability: boolean;
  anmatCategory?: string;
  presentationDescription: string;
  concentration?: string;
  form?: string;
  unitsPerPackage: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(filter?: ProductFilter): Observable<Product[]> {
    let params = new HttpParams();
    if (filter?.gtin) {
      params = params.set('gtin', filter.gtin);
    }
    if (filter?.name) {
      params = params.set('name', filter.name);
    }
    return this.http.get<Product[]>(this.apiUrl, { params });
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  searchByGtin(gtin: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/search/gtin/${gtin}`);
  }
}
