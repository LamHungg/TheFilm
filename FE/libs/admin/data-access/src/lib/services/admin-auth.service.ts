import { Injectable, computed, signal } from '@angular/core';
import { ADMIN_USER } from '../data/admin-mock';
import { AdminUser } from '../models/admin.models';

const STORAGE_KEY = 'thefilm.admin.session';

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly authenticated = signal(this.readSession());

  readonly user = computed<AdminUser | null>(() =>
    this.authenticated() ? ADMIN_USER : null
  );
  readonly isAuthenticated = computed(() => this.authenticated());

  login(email: string, _password: string): boolean {
    if (!email.trim()) {
      return false;
    }
    this.authenticated.set(true);
    localStorage.setItem(STORAGE_KEY, '1');
    return true;
  }

  logout(): void {
    this.authenticated.set(false);
    localStorage.removeItem(STORAGE_KEY);
  }

  private readSession(): boolean {
    try {
      return localStorage.getItem(STORAGE_KEY) === '1';
    } catch {
      return false;
    }
  }
}
