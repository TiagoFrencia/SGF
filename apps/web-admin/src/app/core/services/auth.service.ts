import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthResponse {
  accessToken: string;
  username: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  
  // Use Angular Signals for reactive state
  currentUser = signal<AuthResponse | null>(null);
  isAuthenticated = signal<boolean>(false);

  constructor(private http: HttpClient, private router: Router) {
    const savedAuth = localStorage.getItem('sgf_auth');
    if (savedAuth) {
      const auth = JSON.parse(savedAuth);
      this.currentUser.set(auth);
      this.isAuthenticated.set(true);
    }
  }

  login(credentials: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('sgf_auth', JSON.stringify(response));
        this.currentUser.set(response);
        this.isAuthenticated.set(true);
      })
    );
  }

  logout() {
    localStorage.removeItem('sgf_auth');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return this.currentUser()?.accessToken || null;
  }
}
