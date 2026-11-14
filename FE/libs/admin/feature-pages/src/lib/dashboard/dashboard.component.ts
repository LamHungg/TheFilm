import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { AdminCatalogService, AdminOpsService } from '@the-film/admin/data-access';
import { formatVnd } from '../shared/admin-formatters';

type RangeKey = 'today' | '7d' | '30d';
type AlertLevel = 'high' | 'mid' | 'info';

interface KpiCell {
  id: string;
  icon: string;
  label: string;
  value: string;
  trend?: string;
  note: string;
}

interface AttentionItem {
  id: string;
  level: AlertLevel;
  icon: string;
  title: string;
  flag: string;
  detail: string;
  action: string;
  link: string;
}

const LEVEL_ORDER: Record<AlertLevel, number> = { high: 0, mid: 1, info: 2 };
/** Suất còn mở bán mà tỉ lệ ghế đã bán dưới ngưỡng này thì coi là bán chậm. */
const SLOW_SALES_RATIO = 0.2;

@Component({
  selector: 'admin-dashboard',
  standalone: true,
  imports: [RouterLink, NzButtonModule, NzDropdownModule, NzIconModule, NzMenuModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly ops = inject(AdminOpsService);
  private readonly catalog = inject(AdminCatalogService);

  readonly ranges: { key: RangeKey; label: string }[] = [
    { key: 'today', label: 'Hôm nay' },
    { key: '7d', label: '7 ngày' },
    { key: '30d', label: '30 ngày' },
  ];
  // Chỉ đổi giao diện; lọc số liệu theo khoảng ngày và theo rạp do service/API nối.
  readonly range = signal<RangeKey>('today');
  readonly cinemaId = signal<number | null>(null);

  readonly cinemas = this.catalog.cinemas;
  readonly cinemaLabel = computed(() => {
    const id = this.cinemaId();
    const found = this.cinemas().find((c) => c.id === id);
    return found ? found.name : `Toàn chuỗi · ${this.cinemas().length} rạp`;
  });

  readonly kpis = computed<KpiCell[]>(() => {
    const s = this.ops.dashboardStats();
    return [
      {
        id: 'revenue',
        icon: 'wallet',
        label: 'Doanh thu hôm nay',
        value: formatVnd(s.revenueToday),
        trend: `+${s.revenueTrend.toLocaleString('vi-VN')}%`,
        note: 'so với kỳ trước',
      },
      {
        id: 'tickets',
        icon: 'tag',
        label: 'Vé đã bán',
        value: s.ticketsSoldToday.toLocaleString('vi-VN'),
        trend: `+${s.ticketTrend.toLocaleString('vi-VN')}%`,
        note: 'so với kỳ trước',
      },
      {
        id: 'occupancy',
        icon: 'pie-chart',
        label: 'Lấp đầy',
        value: `${s.occupancyRate}%`,
        note: `${s.activeFilms} phim đang chiếu`,
      },
      {
        id: 'queue',
        icon: 'inbox',
        label: 'Cần xử lý',
        value: String(s.openOrders + s.openSupportCases),
        note: `${s.openOrders} đơn · ${s.openSupportCases} ca CSKH`,
      },
    ];
  });

  readonly attention = computed<AttentionItem[]>(() => {
    const items: AttentionItem[] = [];
    const now = Date.now();

    // Chỉ suất chưa chiếu: suất đã qua giờ bán chậm không còn gì để làm.
    for (const s of this.catalog.screenings().filter((x) => new Date(x.startAt).getTime() > now)) {
      const where = `${this.short(s.cinemaName)}, ${s.roomName}`;
      const time = this.time(s.startAt);
      if (
        (s.status === 'ON_SALE' || s.status === 'SCHEDULED') &&
        s.totalSeats > 0 &&
        s.soldSeats / s.totalSeats < SLOW_SALES_RATIO
      ) {
        items.push({
          id: `slow-${s.id}`,
          level: 'high',
          icon: 'fall',
          title: `${s.filmTitle} · ${time} · ${where}`,
          flag: 'Bán chậm',
          detail: `mới bán ${s.soldSeats} / ${s.totalSeats} ghế (${this.pct(s.soldSeats, s.totalSeats)}%)`,
          action: 'Mở suất',
          link: '/screenings',
        });
      } else if (s.status === 'SOLD_OUT') {
        items.push({
          id: `full-${s.id}`,
          level: 'info',
          icon: 'tag',
          title: `${s.filmTitle} · ${time} · ${where} đã hết vé`,
          flag: '',
          detail: `${s.soldSeats} / ${s.totalSeats} ghế · cân nhắc thêm suất`,
          action: 'Thêm suất',
          link: '/screenings',
        });
      }
    }

    for (const c of this.cinemas().filter((x) => x.status === 'MAINTENANCE')) {
      items.push({
        id: `cinema-${c.id}`,
        level: 'mid',
        icon: 'tool',
        title: `${c.name} đang bảo trì`,
        flag: 'Kiểm tra suất',
        detail: 'các suất đã lên lịch cần dời phòng hoặc huỷ',
        action: 'Xem rạp',
        link: '/cinemas',
      });
    }

    for (const c of this.ops
      .supportCases()
      .filter((x) => x.priority === 'HIGH' && ['OPEN', 'IN_PROGRESS', 'WAITING'].includes(x.status))) {
      items.push({
        id: `support-${c.id}`,
        level: 'high',
        icon: 'customer-service',
        title: `${c.subject} · ${c.customerName}`,
        flag: 'Ưu tiên cao',
        detail: `${c.ticketCode} · qua ${c.channel.toLowerCase()}`,
        action: 'Xử lý',
        link: '/support',
      });
    }

    const openOrders = this.ops.dashboardStats().openOrders;
    if (openOrders > 0) {
      items.push({
        id: 'orders',
        level: 'mid',
        icon: 'clock-circle',
        title: `${openOrders} đơn chờ thanh toán`,
        flag: '',
        detail: 'đang giữ ghế, kiểm tra đơn quá hạn',
        action: 'Xem đơn',
        link: '/orders',
      });
    }

    return items.sort((a, b) => LEVEL_ORDER[a.level] - LEVEL_ORDER[b.level]);
  });
  readonly attentionTop = computed(() => this.attention().slice(0, 6));

  readonly occupancy = computed(() => {
    const maintenance = this.cinemas()
      .filter((c) => c.status === 'MAINTENANCE')
      .map((c) => c.name);
    return [...this.ops.cinemaOccupancy()]
      .sort((a, b) => b.occupancy - a.occupancy)
      .map((o) => ({
        ...o,
        maintenance: maintenance.some((name) => name.endsWith(o.cinemaName)),
      }));
  });

  readonly upcoming = computed(() =>
    [...this.catalog.screenings()]
      .filter(
        (s) =>
          s.status !== 'CANCELLED' && s.status !== 'COMPLETED' && new Date(s.startAt).getTime() > Date.now()
      )
      .sort((a, b) => a.startAt.localeCompare(b.startAt))
      .slice(0, 6)
  );

  // shortcut: số doanh thu theo ngày đang cố định (triệu đồng), thay bằng API báo cáo khi có.
  readonly revenueDays = [
    { tick: 'T2', value: 118 },
    { tick: 'T3', value: 132 },
    { tick: 'T4', value: 126 },
    { tick: 'T5', value: 148 },
    { tick: 'T6', value: 162 },
    { tick: 'T7', value: 188 },
    { tick: 'CN', value: 176 },
  ];
  readonly revenueMax = Math.max(...this.revenueDays.map((d) => d.value));
  readonly revenueTotal = this.revenueDays.reduce((sum, d) => sum + d.value, 0);

  short(cinemaName: string): string {
    return cinemaName.replace(/^THEFILM\s+/, '');
  }

  pct(sold: number, total: number): number {
    return total > 0 ? Math.round((sold / total) * 100) : 0;
  }

  time(iso: string): string {
    return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(
      new Date(iso)
    );
  }

  day(iso: string): string {
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(
      new Date(iso)
    );
  }
}
