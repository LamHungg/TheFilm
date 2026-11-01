import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('@the-film/customer/feature-movies').then((m) => m.HomeComponent),
  },
  {
    path: 'movies',
    loadComponent: () =>
      import('@the-film/customer/feature-movies').then((m) => m.MovieListComponent),
  },
  {
    path: 'movies/:slug',
    loadComponent: () =>
      import('@the-film/customer/feature-movies').then((m) => m.MovieDetailComponent),
  },
  {
    path: 'cinemas',
    loadComponent: () =>
      import('@the-film/customer/feature-movies').then((m) => m.CinemasComponent),
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('@the-film/customer/feature-member').then((m) => m.AuthComponent),
  },
  {
    path: 'booking/:showtimeId',
    loadComponent: () =>
      import('@the-film/customer/feature-booking').then((m) => m.BookingWorkflowComponent),
  },
  {
    path: 'my-tickets',
    loadComponent: () =>
      import('@the-film/customer/feature-member').then((m) => m.MyTicketsComponent),
  },
  {
    path: 'member',
    loadComponent: () =>
      import('@the-film/customer/feature-member').then((m) => m.MemberProfileComponent),
  },
  {
    path: 'support',
    loadComponent: () =>
      import('@the-film/customer/feature-support').then((m) => m.SupportCenterComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
