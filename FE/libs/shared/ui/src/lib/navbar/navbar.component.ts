import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  isMobileMenuOpen = false;
  searchQuery = '';

  constructor(
    private router: Router,
    readonly authService: AuthService,
  ) {}

  login(): void {
    this.closeMobileMenu();
    this.authService.login();
  }

  logout(): void {
    this.closeMobileMenu();
    this.authService.logout();
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/movies'], { queryParams: { q: this.searchQuery.trim() } });
    }
  }
}
