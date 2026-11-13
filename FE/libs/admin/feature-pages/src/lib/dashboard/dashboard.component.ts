import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AdminCatalogService, AdminOpsService } from '@the-film/admin/data-access';
import { formatDateTime, formatVnd, statusLabel } from '../shared/admin-formatters';

type RangeKey = '7d' | '30d' | '90d' | '12m';

interface KpiCard {
  id: string;
  label: string;
  value: string;
  delta: string;
  up: boolean;
  spark: number[];
}

@Component({
  selector: 'admin-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    NzProgressModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  readonly statusLabel = statusLabel;
  private readonly ops = inject(AdminOpsService);
  private readonly catalog = inject(AdminCatalogService);

  readonly range = signal<RangeKey>('30d');
  readonly ranges: RangeKey[] = ['7d', '30d', '90d', '12m'];

  readonly stats = this.ops.dashboardStats;
  readonly occupancy = this.ops.cinemaOccupancy;
  readonly recentOrders = computed(() => this.ops.orders().slice(0, 5));
  readonly upcoming = computed(() =>
    [...this.catalog.screenings()]
      .sort((a, b) => a.startAt.localeCompare(b.startAt))
      .slice(0, 4)
  );

  readonly kpis = computed<KpiCard[]>(() => {
    const s = this.stats();
    return [
      {
        id: 'revenue',
        label: 'Doanh thu',
        value: formatVnd(s.revenueToday),
        delta: `+${s.revenueTrend}% vs kỳ trước`,
        up: true,
        spark: [42, 48, 45, 52, 58, 55, 63, 70, 68, 74, 80, 86],
      },
      {
        id: 'tickets',
        label: 'Vé đã bán',
        value: s.ticketsSoldToday.toLocaleString('vi-VN'),
        delta: `+${s.ticketTrend}% vs kỳ trước`,
        up: true,
        spark: [30, 34, 32, 40, 44, 42, 48, 53, 50, 57, 61, 66],
      },
      {
        id: 'occupancy',
        label: 'Lấp đầy',
        value: `${s.occupancyRate}%`,
        delta: `${s.activeFilms} phim đang chiếu`,
        up: true,
        spark: [55, 58, 60, 59, 64, 67, 70, 72, 71, 75, 77, 78],
      },
      {
        id: 'queue',
        label: 'Cần xử lý',
        value: String(s.openOrders + s.openSupportCases),
        delta: `${s.openOrders} đơn · ${s.openSupportCases} CSKH`,
        up: false,
        spark: [18, 22, 20, 28, 25, 30, 27, 33, 31, 36, 34, 39],
      },
    ];
  });

  readonly chartPoints = [
    { tick: 'T2', current: 118, previous: 96 },
    { tick: 'T3', current: 132, previous: 110 },
    { tick: 'T4', current: 126, previous: 118 },
    { tick: 'T5', current: 148, previous: 122 },
    { tick: 'T6', current: 162, previous: 140 },
    { tick: 'T7', current: 188, previous: 155 },
    { tick: 'CN', current: 176, previous: 160 },
  ];

  readonly formatVnd = formatVnd;
  readonly formatDateTime = formatDateTime;

  sparkPoints(values: number[]): string {
    const max = Math.max(...values);
    const min = Math.min(...values);
    const span = Math.max(max - min, 1);
    return values
      .map((v, i) => {
        const x = (i / (values.length - 1)) * 100;
        const y = 28 - ((v - min) / span) * 22;
        return `${x},${y}`;
      })
      .join(' ');
  }

  chartPath(key: 'current' | 'previous'): string {
    const values = this.chartPoints.map((p) => p[key]);
    const max = Math.max(...values) * 1.08;
    return values
      .map((v, i) => {
        const x = (i / (values.length - 1)) * 100;
        const y = 100 - (v / max) * 78 - 8;
        return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
      })
      .join(' ');
  }

  chartArea(): string {
    const line = this.chartPath('current');
    return `${line} L 100 100 L 0 100 Z`;
  }

  orderColor(status: string): string {
    switch (status) {
      case 'PAID':
        return 'success';
      case 'PENDING_PAYMENT':
        return 'warning';
      case 'REFUNDED':
        return 'processing';
      case 'EXPIRED':
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  }
}
