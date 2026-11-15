import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin-shell/admin-shell.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {
        path: '',
        redirectTo: 'overview',
        pathMatch: 'full',
      },
      {
        path: 'overview',
        loadComponent: () =>
          import('./views/overview/admin-overview.component').then(
            (m) => m.AdminOverviewComponent
          ),
      },
      {
        path: 'organizations',
        loadComponent: () =>
          import('./views/organizations/admin-organizations.component').then(
            (m) => m.AdminOrganizationsComponent
          ),
      },
      {
        path: 'movies',
        loadComponent: () =>
          import('./views/movies/admin-movies.component').then(
            (m) => m.AdminMoviesComponent
          ),
      },
      {
        path: 'rooms-seats',
        loadComponent: () =>
          import('./views/rooms/admin-rooms.component').then(
            (m) => m.AdminRoomsComponent
          ),
      },
      {
        path: 'showtimes',
        loadComponent: () =>
          import('./views/showtimes/admin-showtimes.component').then(
            (m) => m.AdminShowtimesComponent
          ),
      },
      {
        path: 'pricing',
        loadComponent: () =>
          import('./views/pricing/admin-pricing.component').then(
            (m) => m.AdminPricingComponent
          ),
      },
      {
        path: 'ticketing',
        loadComponent: () =>
          import('./views/ticketing/admin-ticketing.component').then(
            (m) => m.AdminTicketingComponent
          ),
      },
      {
        path: 'fnb',
        loadComponent: () =>
          import('./views/fnb/admin-fnb.component').then(
            (m) => m.AdminFnbComponent
          ),
      },
      {
        path: 'staff',
        loadComponent: () =>
          import('./views/staff/admin-staff.component').then(
            (m) => m.AdminStaffComponent
          ),
      },
      {
        path: 'crm',
        loadComponent: () =>
          import('./views/crm/admin-crm.component').then(
            (m) => m.AdminCrmComponent
          ),
      },
      {
        path: 'tax-invoices',
        loadComponent: () =>
          import('./views/tax-invoices/admin-tax-invoices.component').then(
            (m) => m.AdminTaxInvoicesComponent
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./views/reports/admin-reports.component').then(
            (m) => m.AdminReportsComponent
          ),
      },
      {
        path: 'system',
        loadComponent: () =>
          import('./views/system/admin-system.component').then(
            (m) => m.AdminSystemComponent
          ),
      },
    ],
  },
];
