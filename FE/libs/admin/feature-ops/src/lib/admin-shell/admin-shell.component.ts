import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { AdminAuthService } from '../services/admin-auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  moduleCode: string;
}

@Component({
  selector: 'thefilm-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent implements OnInit {
  readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);
  permissionsLoaded = false;
  navItems: NavItem[] = [
    { path: '/admin/overview', label: 'Tổng quan', icon: '⌂', moduleCode: 'OVERVIEW' },
    { path: '/admin/organizations', label: 'Tổ chức & rạp', icon: '◉', moduleCode: 'ORGANIZATIONS' },
    { path: '/admin/movies', label: 'Phim', icon: '▣', moduleCode: 'MOVIES' },
    { path: '/admin/rooms-seats', label: 'Phòng & ghế', icon: '▦', moduleCode: 'ROOMS' },
    { path: '/admin/showtimes', label: 'Lịch chiếu', icon: '▤', moduleCode: 'SHOWTIMES' },
    { path: '/admin/pricing', label: 'Giá & ưu đãi', icon: '₫', moduleCode: 'PRICING' },
    { path: '/admin/vouchers', label: 'Voucher', icon: '◇', moduleCode: 'PRICING' },
    { path: '/admin/ticketing', label: 'Bán vé', icon: '▣', moduleCode: 'TICKETING' },
    { path: '/admin/fnb', label: 'F&B & kho', icon: '◌', moduleCode: 'FNB' },
    { path: '/admin/staff', label: 'Nhân sự', icon: '♙', moduleCode: 'STAFF' },
    { path: '/admin/crm', label: 'CSKH', icon: '✉', moduleCode: 'CRM' },
    { path: '/admin/tax-invoices', label: 'Thuế & hóa đơn', icon: '▤', moduleCode: 'TAX_INVOICES' },
    { path: '/admin/reports', label: 'Báo cáo', icon: '▥', moduleCode: 'REPORTS' },
    { path: '/admin/system', label: 'Hệ thống', icon: '⚙', moduleCode: 'SYSTEM' },
  ];

  ngOnInit(): void {
    this.auth.refreshPermissions().subscribe({
      next: (modules) => this.applyPermissions(modules),
      error: () => {
        const roles = this.auth.currentUser()?.roles || [];
        this.applyPermissions(roles.some((role) => ['CHAIN_ADMIN', 'ROLE_CHAIN_ADMIN'].includes(role))
          ? this.navItems.map((item) => item.moduleCode)
          : []);
      },
    });
  }

  get visibleNavItems(): NavItem[] {
    return this.navItems.filter((item) => this.auth.currentModules().includes(item.moduleCode));
  }

  private applyPermissions(modules: string[]): void {
    this.auth.currentModules.set(modules);
    this.permissionsLoaded = true;
    const currentPath = this.router.url.split('?')[0];
    const currentItem = this.navItems.find((item) => item.path === currentPath);
    if (currentItem && modules.includes(currentItem.moduleCode)) return;
    const firstAllowed = this.navItems.find((item) => modules.includes(item.moduleCode));
    if (firstAllowed) void this.router.navigateByUrl(firstAllowed.path);
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }

  get initials(): string {
    const name = this.auth.currentUser()?.displayName || this.auth.currentUser()?.email || 'NV';
    const parts = name.trim().split(/[\s@.]+/).filter(Boolean);
    return parts.length > 1 ? `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase() : name.slice(0, 2).toUpperCase();
  }
}
