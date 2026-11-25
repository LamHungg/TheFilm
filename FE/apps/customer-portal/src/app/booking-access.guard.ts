import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@the-film/customer/data-access';

export const bookingAccessGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const authUrl = router.createUrlTree(['/auth'], { queryParams: { returnUrl: state.url } });

  if (!auth.isAuthenticated()) return authUrl;

  return true;
};
