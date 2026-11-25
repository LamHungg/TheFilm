import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { Observable, map, of, tap, timeout } from 'rxjs';

const ADMIN_SESSION_KEY = 'thefilm.admin.session';

export interface AdminSessionUser {
  email: string;
  displayName: string;
  roles: string[];
  employeeId?: number | null;
}

interface PermissionResponse { moduleCodes: string[]; fullAccess: boolean; }

interface AdminLoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AdminSessionUser;
}

interface ApiResponse<T> { data: T; }

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly http = inject(HttpClient);
  private readonly browser = isPlatformBrowser(inject(PLATFORM_ID));
  readonly currentUser = signal<AdminSessionUser | null>(null);
  readonly currentModules = signal<string[]>([]);
  private readonly token = signal<string | null>(null);
  private expiresAt: number | null = null;

  constructor() { this.restore(); }

  login(credentials: { email: string; password: string }): Observable<AdminSessionUser> {
    return this.http.post<ApiResponse<AdminLoginResponse>>('/api/v1/admin/auth/login', credentials).pipe(
      map((response) => response.data),
      timeout({ first: 30000 }),
      tap((session) => this.setSession(session)),
      map((session) => session.user),
    );
  }

  isAuthenticated(): boolean {
    if (!this.token() || (this.expiresAt !== null && this.expiresAt <= Date.now())) {
      this.clear();
      return false;
    }
    return true;
  }

  getAccessToken(): string | null { return this.isAuthenticated() ? this.token() : null; }

  refreshPermissions(): Observable<string[]> {
    const accessToken = this.getAccessToken();
    if (!accessToken) {
      this.currentModules.set([]);
      return of([]);
    }
    return this.http.get<ApiResponse<PermissionResponse>>('/api/v1/admin/permissions/current', {
      headers: new HttpHeaders({ Authorization: `Bearer ${accessToken}` }),
    }).pipe(
      map((response) => response.data?.moduleCodes || []),
      tap((modules) => this.currentModules.set(modules)),
    );
  }

  logout(): void { this.clear(); }

  private setSession(session: AdminLoginResponse): void {
    this.token.set(session.accessToken);
    this.currentUser.set(session.user);
    this.currentModules.set([]);
    this.expiresAt = Date.now() + Math.max(1, session.expiresIn || 86400) * 1000;
    if (this.browser) {
      sessionStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify({
        accessToken: session.accessToken,
        expiresAt: this.expiresAt,
        user: session.user,
      }));
    }
  }

  private restore(): void {
    if (!this.browser) return;
    try {
      const raw = sessionStorage.getItem(ADMIN_SESSION_KEY);
      if (!raw) return;
      const session = JSON.parse(raw) as { accessToken?: string; expiresAt?: number; user?: AdminSessionUser };
      if (!session.accessToken || !session.user || !Number.isFinite(session.expiresAt)
          || (session.expiresAt as number) <= Date.now()) {
        this.clear();
        return;
      }
      this.token.set(session.accessToken);
      this.currentUser.set(session.user);
      this.expiresAt = session.expiresAt as number;
    } catch {
      this.clear();
    }
  }

  private clear(): void {
    this.token.set(null);
    this.currentUser.set(null);
    this.currentModules.set([]);
    this.expiresAt = null;
    if (this.browser) sessionStorage.removeItem(ADMIN_SESSION_KEY);
  }
}
