import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { AdminAuthService, AdminOpsService } from '@the-film/admin/data-access';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  badge?: boolean;
}

const NAV_MAIN: NavItem[] = [
  { path: '/dashboard', label: 'Tổng quan', icon: 'dashboard' },
  { path: '/films', label: 'Phim', icon: 'video-camera' },
  { path: '/cinemas', label: 'Rạp & phòng', icon: 'shop' },
  { path: '/screenings', label: 'Suất chiếu', icon: 'calendar' },
];

const NAV_OPS: NavItem[] = [
  { path: '/orders', label: 'Đơn hàng', icon: 'shopping-cart' },
  { path: '/promotions', label: 'Khuyến mãi', icon: 'gift' },
  { path: '/support', label: 'CSKH', icon: 'customer-service', badge: true },
];

const TITLE_MAP: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': {
    title: 'Tổng quan',
    subtitle: 'Doanh thu, suất chiếu và hàng đợi vận hành hôm nay',
  },
  '/films': { title: 'Phim', subtitle: 'Danh mục phim, duyệt và phát hành' },
  '/cinemas': { title: 'Rạp & phòng', subtitle: 'Chuỗi rạp và cấu hình phòng chiếu' },
  '/screenings': { title: 'Suất chiếu', subtitle: 'Lịch chiếu theo rạp và phòng' },
  '/orders': { title: 'Đơn hàng', subtitle: 'Vé, thanh toán và hoàn tiền' },
  '/promotions': { title: 'Khuyến mãi', subtitle: 'Chiến dịch giảm giá và voucher áp dụng toàn chuỗi' },
  '/support': { title: 'CSKH', subtitle: 'Theo dõi và xử lý yêu cầu hỗ trợ của khách hàng' },
};

@Component({
  selector: 'admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    FormsModule,
    NzLayoutModule,
    NzMenuModule,
    NzIconModule,
    NzAvatarModule,
    NzDropdownModule,
    NzInputModule,
    NzButtonModule,
    NzTooltipModule,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AdminAuthService);
  private readonly ops = inject(AdminOpsService);

  readonly navMain = NAV_MAIN;
  readonly navOps = NAV_OPS;
  readonly user = this.auth.user;
  search = '';

  readonly openSupport = computed(
    () =>
      this.ops
        .supportCases()
        .filter((c) => ['OPEN', 'IN_PROGRESS', 'WAITING'].includes(c.status))
        .length
  );

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url)
    ),
    { initialValue: this.router.url }
  );

  readonly pageMeta = computed(() => {
    const path =
      '/' + (this.url().split('?')[0].split('/').filter(Boolean)[0] ?? 'dashboard');
    return (
      TITLE_MAP[path] ?? {
        title: 'THEFILM Admin',
        subtitle: 'Operations console',
      }
    );
  });

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
