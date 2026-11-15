import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'thefilm-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent {
  navItems: NavItem[] = [
    { path: '/admin/overview', label: 'Tổng quan', icon: '⌂' },
    { path: '/admin/organizations', label: 'Tổ chức & rạp', icon: '◉' },
    { path: '/admin/movies', label: 'Phim', icon: '▣' },
    { path: '/admin/rooms-seats', label: 'Phòng & ghế', icon: '▦' },
    { path: '/admin/showtimes', label: 'Lịch chiếu', icon: '▤' },
    { path: '/admin/pricing', label: 'Giá & ưu đãi', icon: '₫' },
    { path: '/admin/ticketing', label: 'Bán vé', icon: '▣' },
    { path: '/admin/fnb', label: 'F&B & kho', icon: '◌' },
    { path: '/admin/staff', label: 'Nhân sự', icon: '♙' },
    { path: '/admin/crm', label: 'CSKH', icon: '✉' },
    { path: '/admin/tax-invoices', label: 'Thuế & hóa đơn', icon: '▤' },
    { path: '/admin/reports', label: 'Báo cáo', icon: '▥' },
    { path: '/admin/system', label: 'Hệ thống', icon: '⚙' },
  ];
}
