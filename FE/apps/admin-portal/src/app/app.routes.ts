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
      {
        path: 'film-releases',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.FilmReleasesComponent),
      },
      {
        path: 'room-config',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.RoomConfigComponent),
      },
      {
        path: 'cinemas/rooms/:roomId',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.SeatLayoutComponent),
      },
      {
        path: 'pricing',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.PricingComponent),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.ReportsComponent),
      },
      {
        path: 'refunds',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.RefundsComponent),
      },
      {
        path: 'members',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.MembersComponent),
      },
      {
        path: 'fnb',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.FnbComponent),
      },
      {
        path: 'invoices',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.InvoicesComponent),
      },
      {
        path: 'staff',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.StaffComponent),
      },
      {
        path: 'shifts',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.ShiftsComponent),
      },
      {
        path: 'organization',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.OrganizationComponent),
      },
      {
        path: 'audit',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.AuditComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('@the-film/admin/feature-pages').then((m) => m.SettingsComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
