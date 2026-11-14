import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { AdminCatalogService } from '@the-film/admin/data-access';
import { formatVnd } from '../shared/admin-formatters';

type Range = '7d' | '30d' | 'month' | 'quarter';

// shortcut: số liệu báo cáo cố định (mock), thay bằng API báo cáo khi BE có (lọc theo rạp + khoảng ngày).
const DAILY = [142, 118, 126, 131, 155, 198, 214, 120, 132, 126, 148, 162, 188, 176];
const BY_FILM = [
  { film: 'Tầng Mây Cuối Cùng', shows: 412, tickets: 48210, revenue: 5_284_500_000, occupancy: 84, sharePct: 50 },
  { film: 'Mùa Hè Bất Tận', shows: 286, tickets: 26840, revenue: 2_946_200_000, occupancy: 71, sharePct: 48 },
  { film: 'Khu Rừng Kỳ Diệu', shows: 64, tickets: 3120, revenue: 268_900_000, occupancy: 38, sharePct: 45 },
  { film: 'Hồ Sơ Đêm Mưa', shows: 0, tickets: 0, revenue: 0, occupancy: 0, sharePct: 55 },
];
const BY_CINEMA = [
  { cinema: 'THEFILM Royal City', tickets: 26410, ticketRevenue: 2_986_000_000, fnbRevenue: 812_400_000, occupancy: 88 },
  { cinema: 'THEFILM Vincom Đồng Khởi', tickets: 24180, ticketRevenue: 3_104_700_000, fnbRevenue: 905_100_000, occupancy: 83 },
  { cinema: 'THEFILM Aeon Tân Bình', tickets: 19840, ticketRevenue: 1_870_900_000, fnbRevenue: 498_600_000, occupancy: 72 },
  { cinema: 'THEFILM Indochina Đà Nẵng', tickets: 7740, ticketRevenue: 538_000_000, fnbRevenue: 121_300_000, occupancy: 41 },
];
const TOP_FNB = [
  { name: 'Combo Đôi', qty: 18420, revenue: 2_928_780_000 },
  { name: 'Bắp rang caramel size L', qty: 9210, revenue: 727_590_000 },
  { name: 'Coca-Cola 22oz', qty: 14880, revenue: 580_320_000 },
  { name: 'Combo Gold Class', qty: 362, revenue: 104_618_000 },
];

@Component({
  selector: 'admin-reports',
  standalone: true,
  imports: [FormsModule, NzButtonModule, NzIconModule, NzSelectModule, NzTableModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent {
  private readonly catalog = inject(AdminCatalogService);

  readonly cinemas = this.catalog.cinemas;
  readonly cinemaId = signal(0);
  readonly range = signal<Range>('30d');
  readonly ranges: { key: Range; label: string }[] = [
    { key: '7d', label: '7 ngày' },
    { key: '30d', label: '30 ngày' },
    { key: 'month', label: 'Tháng này' },
    { key: 'quarter', label: 'Quý này' },
  ];

  readonly byFilm = BY_FILM;
  readonly byCinema = BY_CINEMA;
  readonly topFnb = TOP_FNB;
  readonly daily = DAILY;
  readonly dailyMax = Math.max(...DAILY);

  readonly totals = computed(() => {
    const ticketRevenue = BY_CINEMA.reduce((s, c) => s + c.ticketRevenue, 0);
    const fnbRevenue = BY_CINEMA.reduce((s, c) => s + c.fnbRevenue, 0);
    const tickets = BY_CINEMA.reduce((s, c) => s + c.tickets, 0);
    return {
      ticketRevenue,
      fnbRevenue,
      tickets,
      avgTicket: Math.round(ticketRevenue / tickets),
      fnbPerHead: Math.round(fnbRevenue / tickets),
    };
  });

  readonly formatVnd = formatVnd;

  short(v: number): string {
    if (v >= 1e9) return `${(v / 1e9).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tỷ`;
    if (v >= 1e6) return `${(v / 1e6).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} tr`;
    return v.toLocaleString('vi-VN');
  }

  num(v: number): string {
    return v.toLocaleString('vi-VN');
  }

  exportFile(): void {
    // Xuất Excel / CSV: BE trả file theo bộ lọc hiện tại.
  }
}
