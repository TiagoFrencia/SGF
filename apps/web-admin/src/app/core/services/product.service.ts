import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  gtin: string;
  name: string;
  description?: string;
  price: number;
  stock: number;
  category?: string;
  laboratory?: string;
  requiresPrescription: boolean;
}

export interface ProductFilter {
  gtin?: string;
  name?: string;
  category?: string;
  page?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(filter?: ProductFilter): Observable<any> {
    let params = new HttpParams();
    if (filter) {
      if (filter.gtin) params = params.set('gtin', filter.gtin);
      if (filter.name) params = params.set('name', filter.name);
      if (filter.category) params = params.set('category', filter.category);
      if (filter.page !== undefined) params = params.set('page', filter.page.toString());
      if (filter.size !== undefined) params = params.set('size', filter.size.toString());
    }
    return this.http.get<any>(this.apiUrl, { params });
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  updateProduct(id: string, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  searchByGtin(gtin: string): Observable<Product | null> {
    return this.http.get<Product | null>(`${this.apiUrl}/search/gtin/${gtin}`);
  }

  getLowStockProducts(threshold: number = 10): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/low-stock?threshold=${threshold}`);
  }

  getExpiringProducts(days: number = 30): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/expiring?days=${days}`);
  }
}
