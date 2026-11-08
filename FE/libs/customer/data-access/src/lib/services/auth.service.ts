import { HttpClient, HttpHeaders } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import {
  computed,
  inject,
  Injectable,
  InjectionToken,
  isDevMode,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthState, OidcConfig, TokenResponse, UserProfile } from '../models/auth.model';

const SESSION_STORAGE_KEY = 'thefilm.auth.session';
const OIDC_TRANSACTION_KEY = 'thefilm.auth.oidc-transaction';
const MOCK_PROFILE: UserProfile = {
  displayName: 'Nguyen Van A',
  email: 'an.nguyen@example.com',
};

type RuntimeAuthWindow = Window & {
  __THEFILM_AUTH_CONFIG__?: Partial<OidcConfig>;
};

/** OIDC runtime settings contain public client configuration only. */
export const AUTH_CONFIG = new InjectionToken<OidcConfig>('AUTH_CONFIG', {
  providedIn: 'root',
  factory: () => {
    const browser = isPlatformBrowser(inject(PLATFORM_ID));
    const redirectUri = browser ? `${window.location.origin}/auth` : '';
    const runtimeConfig = browser
      ? (window as RuntimeAuthWindow).__THEFILM_AUTH_CONFIG__
      : undefined;
    const mockAuthEnabled = runtimeConfig?.mockAuthEnabled ??
      (isDevMode() && !(runtimeConfig?.issuer && runtimeConfig?.clientId));

    return {
      issuer: '',
      clientId: '',
      redirectUri,
      postLogoutRedirectUri: redirectUri,
      scope: 'openid profile email',
      mockAuthEnabled,
      mockAccessToken: 'mock-jwt-token',
      mockUser: MOCK_PROFILE,
      ...runtimeConfig,
    };
  },
});

interface OidcTransaction {
  state: string;
  codeVerifier: string;
  returnUrl: string;
}

interface ApiResponse<T> {
  data: T;
}

export interface AuthUserPayload {
  email: string;
  displayName: string;
  phone?: string | null;
  roles?: string[];
}

export interface AuthResponseBody {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUserPayload;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly config = inject(AUTH_CONFIG);
  private readonly browser = isPlatformBrowser(this.platformId);
  private readonly authState = signal<AuthState>({
    user: null,
    accessToken: null,
    idToken: null,
    expiresAt: null,
    isAuthenticated: false,
  });
  private expiryTimer: ReturnType<typeof setTimeout> | null = null;

  readonly currentUser = computed(() => this.authState().user);
  readonly isAuthenticated = computed(() => this.authState().isAuthenticated);
  readonly token = computed(() => this.authState().accessToken);

  constructor() {
    this.restoreSession();
    if (this.browser) {
      // Defer until construction finishes so the interceptor can safely inject
      // this service while the callback's token request is sent.
      queueMicrotask(() => this.completeAuthorizationCallback());
    }
  }

  login(): Promise<void> {
    if (this.config.mockAuthEnabled) {
      this.signInWithMockProfile();
      return Promise.resolve();
    }
    return this.startOidcFlow();
  }

  loginWithCredentials(credentials: { email: string; password: string }): Observable<UserProfile> {
    return this.http
      .post<ApiResponse<AuthResponseBody>>('/api/v1/auth/login', credentials)
      .pipe(
        map((res) => res.data),
        tap((data) => {
          const userProfile: UserProfile = {
            displayName: data.user.displayName,
            email: data.user.email,
            phone: data.user.phone,
          };
          const expiresAt = Date.now() + (data.expiresIn || 86400) * 1000;
          this.setSession(data.accessToken, userProfile, expiresAt);
        }),
        map((data) => ({
          displayName: data.user.displayName,
          email: data.user.email,
          phone: data.user.phone,
        })),
      );
  }

  register(): Promise<void> {
    if (this.config.mockAuthEnabled) {
      this.signInWithMockProfile();
      return Promise.resolve();
    }
    return this.startOidcFlow('register');
  }

  registerWithCredentials(payload: {
    email: string;
    password: string;
    displayName: string;
    phone?: string;
  }): Observable<UserProfile> {
    return this.http
      .post<ApiResponse<AuthResponseBody>>('/api/v1/auth/register', payload)
      .pipe(
        map((res) => res.data),
        tap((data) => {
          const userProfile: UserProfile = {
            displayName: data.user.displayName,
            email: data.user.email,
            phone: data.user.phone,
          };
          const expiresAt = Date.now() + (data.expiresIn || 86400) * 1000;
          this.setSession(data.accessToken, userProfile, expiresAt);
        }),
        map((data) => ({
          displayName: data.user.displayName,
          email: data.user.email,
          phone: data.user.phone,
        })),
      );
  }

  logout(): void {
    const idToken = this.authState().idToken;
    this.clearSession();

    if (!this.browser || this.config.mockAuthEnabled) return;
    const endpoint = this.endSessionEndpoint();
    if (!endpoint) return;

    const url = this.resolveUrl(endpoint);
    if (idToken) url.searchParams.set('id_token_hint', idToken);
    const redirectUri = this.config.postLogoutRedirectUri || this.defaultRedirectUri();
    if (redirectUri) url.searchParams.set('post_logout_redirect_uri', redirectUri);
    if (this.config.clientId) url.searchParams.set('client_id', this.config.clientId);
    window.location.assign(url.toString());
  }

  /** Clears a rejected session without initiating another IdP redirect. */
  handleUnauthorized(rejectedToken?: string): boolean {
    if (rejectedToken && this.authState().accessToken !== rejectedToken) return false;
    this.clearSession();
    return true;
  }

  getAccessToken(): string | null {
    const state = this.authState();
    if (!state.accessToken) return null;
    if (state.expiresAt !== null && state.expiresAt <= Date.now()) {
      this.clearSession();
      return null;
    }
    return state.accessToken;
  }

  /** Public to support deterministic session setup in application tests. */
  setSession(
    accessToken: string,
    user: UserProfile | null,
    expiresAt: number | null = typeof accessToken === 'string'
      ? this.readTokenExpiry(accessToken)
      : null,
    idToken: string | null = null,
  ): void {
    const token = typeof accessToken === 'string' ? accessToken.trim() : '';
    const resolvedExpiry = expiresAt ?? this.readTokenExpiry(token);
    if (
      !token ||
      (resolvedExpiry !== null &&
        (!Number.isFinite(resolvedExpiry) || resolvedExpiry <= Date.now()))
    ) {
      this.clearSession();
      throw new Error('Cannot create an authenticated session from an invalid or expired token.');
    }

    const state: AuthState = {
      user,
      accessToken: token,
      idToken,
      expiresAt: resolvedExpiry,
      isAuthenticated: true,
    };
    this.authState.set(state);
    this.persistSession(state);
    this.scheduleExpiry(resolvedExpiry);
  }

  getProfile(): Observable<UserProfile> {
    return this.http
      .get<ApiResponse<UserProfile> | UserProfile>('/api/v1/customer/profile')
      .pipe(
        map((response) => ('data' in response ? response.data : response)),
        tap((profile) => this.updateProfile(profile)),
      );
  }

  private signInWithMockProfile(): void {
    const user = this.config.mockUser ?? MOCK_PROFILE;
    const token = this.config.mockAccessToken || 'mock-jwt-token';
    this.setSession(token, user, Date.now() + 60 * 60 * 1000);
  }

  private async startOidcFlow(action?: 'register'): Promise<void> {
    if (!this.browser) {
      throw new Error('OIDC sign-in is available only in a browser.');
    }
    const endpoint = this.authorizationEndpoint();
    if (!endpoint || !this.config.clientId) {
      throw new Error('THEFILM OIDC configuration requires an issuer and clientId.');
    }

    const storage = this.getStorage();
    if (!storage) {
      throw new Error('THEFILM OIDC requires browser session storage for PKCE.');
    }

    try {
      const codeVerifier = this.base64Url(crypto.getRandomValues(new Uint8Array(32)));
      const state = this.base64Url(crypto.getRandomValues(new Uint8Array(24)));
      const challengeBytes = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(codeVerifier),
      );
      const transaction: OidcTransaction = {
        state,
        codeVerifier,
        returnUrl: `${window.location.pathname}${window.location.search}${window.location.hash}`,
      };
      storage.setItem(OIDC_TRANSACTION_KEY, JSON.stringify(transaction));

      const url = this.resolveUrl(endpoint);
      url.searchParams.set('client_id', this.config.clientId);
      url.searchParams.set('response_type', 'code');
      url.searchParams.set('scope', this.config.scope || 'openid profile email');
      url.searchParams.set('redirect_uri', this.config.redirectUri || this.defaultRedirectUri());
      url.searchParams.set('state', state);
      url.searchParams.set('code_challenge', this.base64Url(new Uint8Array(challengeBytes)));
      url.searchParams.set('code_challenge_method', 'S256');
      if (action === 'register') url.searchParams.set('kc_action', 'register');
      window.location.assign(url.toString());
    } catch {
      throw new Error('Unable to start THEFILM OIDC authorization.');
    }
  }

  private completeAuthorizationCallback(): void {
    const callbackUrl = new URL(window.location.href);
    const code = callbackUrl.searchParams.get('code');
    if (!code) {
      if (callbackUrl.searchParams.has('error')) {
        try {
          this.getStorage()?.removeItem(OIDC_TRANSACTION_KEY);
        } catch {
          // The failed callback must not prevent the application from starting.
        }
        this.removeCallbackParameters(callbackUrl);
      }
      return;
    }

    const storage = this.getStorage();
    let transactionText: string | null = null;
    let transaction: OidcTransaction | null = null;
    try {
      transactionText = storage?.getItem(OIDC_TRANSACTION_KEY) ?? null;
      transaction = transactionText ? (JSON.parse(transactionText) as OidcTransaction) : null;
    } catch {
      transaction = null;
    }
    const returnedState = callbackUrl.searchParams.get('state');
    if (!transaction || !returnedState || transaction.state !== returnedState) {
      try {
        storage?.removeItem(OIDC_TRANSACTION_KEY);
      } catch {
        // Ignore storage errors after rejecting an invalid callback.
      }
      this.removeCallbackParameters(callbackUrl);
      return;
    }

    const validTransaction = transaction as OidcTransaction;
    try {
      storage?.removeItem(OIDC_TRANSACTION_KEY);
    } catch {
      // The transaction has already been copied into memory for this callback.
    }
    this.removeCallbackParameters(callbackUrl);
    const endpoint = this.tokenEndpoint();
    if (!endpoint) return;

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: this.config.clientId,
      code,
      redirect_uri: this.config.redirectUri || this.defaultRedirectUri(),
      code_verifier: validTransaction.codeVerifier,
    });
    this.http
      .post<TokenResponse>(endpoint, body.toString(), {
        headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
      })
      .pipe(
        map((response) => {
          if (!response.access_token) throw new Error('OIDC token response has no access_token.');
          return response;
        }),
        switchMap((response) => {
          const expiresAt = response.expires_in !== undefined
            ? Date.now() + response.expires_in * 1000
            : this.readTokenExpiry(response.access_token);
          this.setSession(response.access_token, null, expiresAt, response.id_token ?? null);
          return this.getProfile().pipe(
            catchError(() => of(null)),
            map((profile) => ({ profile, returnUrl: validTransaction.returnUrl })),
          );
        }),
        catchError((error: unknown) => {
          this.clearSession();
          console.error('THEFILM OIDC authorization could not be completed.', error);
          return of(null);
        }),
      )
      .subscribe((result) => {
        if (!result) return;
        if (result.profile) this.updateProfile(result.profile);
        window.location.replace(result.returnUrl || '/');
      });
  }

  private updateProfile(profile: UserProfile): void {
    const state = this.authState();
    const nextState = {
      ...state,
      user: {
        ...profile,
        avatarUrl: profile.avatarUrl ?? state.user?.avatarUrl ?? null,
      },
    };
    this.authState.set(nextState);
    this.persistSession(nextState);
  }

  private restoreSession(): void {
    const storage = this.getStorage();
    if (!storage) return;
    try {
      const stored = storage.getItem(SESSION_STORAGE_KEY);
      if (!stored) return;
      const session = JSON.parse(stored) as AuthState;
      if (
        typeof session.accessToken !== 'string' ||
        !session.accessToken.trim() ||
        (session.expiresAt !== null &&
          (typeof session.expiresAt !== 'number' ||
            !Number.isFinite(session.expiresAt) ||
            session.expiresAt <= Date.now()))
      ) {
        this.clearSession();
        return;
      }
      const restored: AuthState = {
        user: session.user ?? null,
        accessToken: session.accessToken.trim(),
        idToken: session.idToken ?? null,
        expiresAt: session.expiresAt ?? null,
        isAuthenticated: true,
      };
      this.authState.set(restored);
      this.scheduleExpiry(restored.expiresAt);
    } catch {
      this.clearSession();
    }
  }

  private clearSession(): void {
    if (this.expiryTimer !== null) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
    this.authState.set({
      user: null,
      accessToken: null,
      idToken: null,
      expiresAt: null,
      isAuthenticated: false,
    });
    const storage = this.getStorage();
    try {
      storage?.removeItem(SESSION_STORAGE_KEY);
      storage?.removeItem(OIDC_TRANSACTION_KEY);
    } catch {
      // Clearing in-memory state must succeed even when storage is locked.
    }
  }

  private persistSession(state: AuthState): void {
    try {
      this.getStorage()?.setItem(SESSION_STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Keep the in-memory session usable when browser storage is unavailable.
    }
  }

  private scheduleExpiry(expiresAt: number | null): void {
    if (!this.browser) return;
    if (this.expiryTimer !== null) clearTimeout(this.expiryTimer);
    this.expiryTimer = null;
    if (expiresAt === null) return;

    const delay = Math.max(0, expiresAt - Date.now());
    this.expiryTimer = setTimeout(() => {
      if (expiresAt > Date.now()) this.scheduleExpiry(expiresAt);
      else this.clearSession();
    }, Math.min(delay, 2_147_000_000));
  }

  private getStorage(): Storage | null {
    if (!this.browser) return null;
    try {
      return window.sessionStorage;
    } catch {
      return null;
    }
  }

  private authorizationEndpoint(): string | null {
    return this.config.authorizationEndpoint || this.endpointFromIssuer('auth');
  }

  private tokenEndpoint(): string | null {
    return this.config.tokenEndpoint || this.endpointFromIssuer('token');
  }

  private endSessionEndpoint(): string | null {
    return this.config.endSessionEndpoint || this.endpointFromIssuer('logout');
  }

  private endpointFromIssuer(endpoint: 'auth' | 'token' | 'logout'): string | null {
    const issuer = this.config.issuer?.replace(/\/$/, '');
    if (!issuer) return null;
    return `${issuer}/protocol/openid-connect/${endpoint}`;
  }

  private defaultRedirectUri(): string {
    return this.browser ? `${window.location.origin}/auth` : '';
  }

  private resolveUrl(value: string): URL {
    return this.browser ? new URL(value, window.location.origin) : new URL(value);
  }

  private removeCallbackParameters(url: URL): void {
    for (const key of ['code', 'state', 'session_state', 'iss', 'error', 'error_description']) {
      url.searchParams.delete(key);
    }
    window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
  }

  private base64Url(bytes: Uint8Array): string {
    let binary = '';
    for (const byte of bytes) binary += String.fromCharCode(byte);
    return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  private readTokenExpiry(token: string | null | undefined): number | null {
    const payload = this.readTokenClaims(token);
    const expiration = payload?.['exp'];
    return typeof expiration === 'number' ? expiration * 1000 : null;
  }

  private readTokenClaims(token: string | null | undefined): Record<string, unknown> | null {
    if (!token || !this.browser) return null;
    try {
      const encoded = token.split('.')[1];
      if (!encoded) return null;
      const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
      const binary = window.atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
      const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
