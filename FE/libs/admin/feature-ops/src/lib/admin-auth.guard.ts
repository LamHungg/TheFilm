import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { AdminAuthService } from './services/admin-auth.service';

export const adminAuthGuard: CanActivateChildFn = (_route, state) => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/admin/login'], {
    queryParams: { returnUrl: state.url },
  });
};

export const adminLoginGuard: CanActivateFn = () => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? router.createUrlTree(['/admin/overview']) : true;
};
