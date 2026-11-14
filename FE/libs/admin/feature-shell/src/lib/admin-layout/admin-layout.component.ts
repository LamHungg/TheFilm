import { Component, computed, inject, signal } from '@angular/core';
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

const NAV_GROUPS: { label: string; items: NavItem[] }[] = [
  {
    label: 'Vận hành',
    items: [
      { path: '/dashboard', label: 'Tổng quan', icon: 'dashboard' },
      { path: '/films', label: 'Phim', icon: 'video-camera' },
      { path: '/film-releases', label: 'Bản chiếu & phát hành', icon: 'play-circle' },
      { path: '/cinemas', label: 'Rạp & phòng', icon: 'shop' },
      { path: '/room-config', label: 'Loại phòng & ghế', icon: 'appstore' },
      { path: '/screenings', label: 'Suất chiếu', icon: 'calendar' },
      { path: '/pricing', label: 'Giá vé', icon: 'tag' },
    ],
  },
  {
    label: 'Kinh doanh',
    items: [
      { path: '/reports', label: 'Báo cáo', icon: 'bar-chart' },
      { path: '/orders', label: 'Đơn hàng', icon: 'shopping-cart' },
      { path: '/refunds', label: 'Hoàn & đổi vé', icon: 'rollback' },
      { path: '/promotions', label: 'Khuyến mãi', icon: 'gift' },
      { path: '/members', label: 'Thành viên', icon: 'crown' },
      { path: '/fnb', label: 'Bắp nước', icon: 'coffee' },
      { path: '/invoices', label: 'Hoá đơn', icon: 'file-text' },
      { path: '/support', label: 'CSKH', icon: 'customer-service', badge: true },
    ],
  },
  {
    label: 'Nhân sự',
    items: [
      { path: '/staff', label: 'Nhân viên & phân quyền', icon: 'team' },
      { path: '/shifts', label: 'Ca làm & lương', icon: 'schedule' },
    ],
  },
  {
    label: 'Hệ thống',
    items: [
      { path: '/organization', label: 'Tổ chức', icon: 'bank' },
      { path: '/audit', label: 'Nhật ký thao tác', icon: 'history' },
      { path: '/settings', label: 'Cài đặt', icon: 'setting' },
    ],
  },
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
  '/film-releases': { title: 'Bản chiếu & phát hành', subtitle: 'Định dạng, âm thanh và quyền phát hành của từng phim' },
  '/room-config': { title: 'Loại phòng & ghế', subtitle: 'Loại phòng, loại ghế, phụ thu và bảo trì ghế' },
  '/pricing': { title: 'Giá vé', subtitle: 'Quy tắc giá theo rạp, loại phòng, loại ghế và khung giờ' },
  '/reports': { title: 'Báo cáo', subtitle: 'Doanh thu vé, bắp nước và lấp đầy theo phim, rạp' },
  '/refunds': { title: 'Hoàn & đổi vé', subtitle: 'Duyệt yêu cầu hoàn tiền và đổi suất của khách' },
  '/members': { title: 'Thành viên', subtitle: 'Khách hàng thành viên, hạng và điểm thưởng' },
  '/fnb': { title: 'Bắp nước', subtitle: 'Sản phẩm, giá bán và tồn kho tại từng rạp' },
  '/invoices': { title: 'Hoá đơn', subtitle: 'Hoá đơn điện tử và thuế suất' },
  '/staff': { title: 'Nhân viên & phân quyền', subtitle: 'Tài khoản, vai trò và phạm vi rạp của nhân viên' },
  '/shifts': { title: 'Ca làm & lương', subtitle: 'Xếp ca, chấm công, nghỉ phép và bảng lương' },
  '/organization': { title: 'Tổ chức', subtitle: 'Chuỗi rạp và pháp nhân xuất hoá đơn' },
  '/audit': { title: 'Nhật ký thao tác', subtitle: 'Ai đã làm gì, lúc nào, từ đâu' },
  '/settings': { title: 'Cài đặt', subtitle: 'Quy định bán vé, hoàn vé, thanh toán và hoá đơn' },
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

  readonly navGroups = NAV_GROUPS;
  readonly user = this.auth.user;
  search = '';
  /** Menu trượt trên màn hẹp (dưới 900px sidebar ẩn). */
  readonly menuOpen = signal(false);

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
