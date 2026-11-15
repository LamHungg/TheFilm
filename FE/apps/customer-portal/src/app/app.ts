import { Component, inject } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { NavbarComponent, FooterComponent } from '@the-film/shared/ui';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  title = 'customer-portal';
  private router = inject(Router);

  isAdminRoute = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects.startsWith('/admin'))
    ),
    {
      initialValue:
        typeof window !== 'undefined'
          ? window.location.pathname.startsWith('/admin')
          : false,
    }
  );
}
