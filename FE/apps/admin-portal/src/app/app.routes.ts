import { Route } from '@angular/router';
import { adminAuthGuard, adminGuestGuard } from './auth.guard';

export const appRoutes: Route[] = [
  {
    path: 'login',
    canActivate: [adminGuestGuard],
    loadComponent: () =>
      import('@the-film/admin/feature-pages').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [adminAuthGuard],
    loadComponent: () =>
      import('@the-film/admin/feature-shell').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.DashboardComponent),
      },
      {
        path: 'films',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.FilmsComponent),
      },
      {
        path: 'cinemas',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.CinemasComponent),
      },
      {
        path: 'screenings',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.ScreeningsComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.OrdersComponent),
      },
      {
        path: 'promotions',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.PromotionsComponent),
      },
      {
        path: 'support',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.SupportComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
