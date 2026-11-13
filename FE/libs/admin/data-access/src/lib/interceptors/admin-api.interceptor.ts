import { HttpInterceptorFn } from '@angular/common/http';

/** Endpoint upload poster/trailer; tệp trả về được phục vụ tại /media/**. */
export const ADMIN_MEDIA_UPLOAD_URL = '/api/v1/admin/media';

// shortcut: token cố định của profile BE "devauth" (DevAdminAuthFilter); thay bằng access token Keycloak khi admin có đăng nhập thật.
const DEV_ADMIN_TOKEN = 'mock-admin-token';

export const adminApiInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/v1/admin/')) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${DEV_ADMIN_TOKEN}` } }));
};
