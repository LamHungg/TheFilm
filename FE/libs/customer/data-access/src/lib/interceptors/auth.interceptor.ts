import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AUTH_CONFIG, AuthService } from '../services/auth.service';

function isBackendRequest(url: string, apiBaseUrl: string | undefined, browser: boolean): boolean {
  const appOrigin = browser ? window.location.origin : 'http://localhost';
  try {
    const requestUrl = new URL(url, appOrigin);
    if (apiBaseUrl) {
      const backendUrl = new URL(apiBaseUrl, appOrigin);
      const basePath = backendUrl.pathname.replace(/\/$/, '') || '/api';
      const targetsConfiguredBackend = requestUrl.origin === backendUrl.origin &&
        (requestUrl.pathname === basePath || requestUrl.pathname.startsWith(`${basePath}/`));
      const targetsSameOriginApi = requestUrl.origin === appOrigin &&
        /^\/api(?:\/|$)/.test(requestUrl.pathname);
      return targetsConfiguredBackend || targetsSameOriginApi;
    }

    return requestUrl.origin === appOrigin && /^\/api(?:\/|$)/.test(requestUrl.pathname);
  } catch {
    return false;
  }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router, { optional: true });
  const config = inject(AUTH_CONFIG);
  const browser = isPlatformBrowser(inject(PLATFORM_ID));
  const token = isBackendRequest(request.url, config.apiBaseUrl, browser)
    ? authService.getAccessToken()
    : null;
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (token && error instanceof HttpErrorResponse && error.status === 401) {
        const clearedSession = authService.handleUnauthorized(token);
        if (clearedSession && browser && router && !router.url.startsWith('/auth')) {
          void router.navigateByUrl('/auth').catch(() => false);
        }
      }
      // A 403 is an authorization decision and does not invalidate the session.
      return throwError(() => error);
    }),
  );
};
