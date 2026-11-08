export interface UserProfile {
  appUserId?: number | null;
  subject?: string;
  email?: string | null;
  displayName: string;
  preferredCinemaId?: number | null;
  preferredLocale?: string | null;
  dateOfBirth?: string | null;
  avatarUrl?: string | null;
}

export interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  idToken: string | null;
  expiresAt: number | null;
  isAuthenticated: boolean;
}

/** Public OIDC settings. These values may be supplied through AUTH_CONFIG or
 * window.__THEFILM_AUTH_CONFIG__ at runtime; no client secret belongs here. */
export interface OidcConfig {
  issuer: string;
  clientId: string;
  redirectUri?: string;
  postLogoutRedirectUri?: string;
  scope?: string;
  authorizationEndpoint?: string;
  tokenEndpoint?: string;
  endSessionEndpoint?: string;
  apiBaseUrl?: string;
  mockAuthEnabled?: boolean;
  mockAccessToken?: string;
  mockUser?: UserProfile;
}

export interface TokenResponse {
  access_token: string;
  token_type?: string;
  expires_in?: number;
  refresh_token?: string;
  id_token?: string;
  scope?: string;
}
