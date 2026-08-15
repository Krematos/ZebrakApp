import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly USER_KEY = 'zebrak_user_data';

  readonly currentUser = signal<User | null>(this.getStoredUser());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ROLE_ADMIN');

  constructor(private http: HttpClient) {
    this.fetchCurrentUser().subscribe();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((res) => {
        this.saveUser(res.user);
      })
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap((res) => {
        this.saveUser(res.user);
      })
    );
  }

  logout(): void {
    this.http.post('/api/auth/logout', {}).subscribe({
      next: () => this.clearLocalState(),
      error: () => this.clearLocalState(),
    });
  }

  private clearLocalState(): void {
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
  }

  private saveUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  fetchCurrentUser(): Observable<User | null> {
    return this.http.get<User>('/api/auth/me').pipe(
      tap((user) => {
        this.currentUser.set(user);
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
      }),
      catchError(() => {
        this.clearLocalState();
        return of(null);
      })
    );
  }
}
