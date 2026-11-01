import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
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
}
