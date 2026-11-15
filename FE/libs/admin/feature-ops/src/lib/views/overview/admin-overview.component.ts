import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  AdminOpsService,
  FilmAdminDto,
  ScreeningAdminDto,
  CinemaAdminDto,
} from '../../services/admin-ops.service';

interface KpiCard {
  label: string;
  value: string;
  delta: string;
}

interface ShowtimeRow {
  timeMovie: string;
  room: string;
  seats: string;
  status: string;
  isGreen: boolean;
}

interface TaskItem {
  title: string;
  sub: string;
  status: string;
}

interface ChartDay {
  day: string;
  height: number;
  color: string;
}

@Component({
  selector: 'thefilm-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-overview.component.html',
  styleUrl: './admin-overview.component.scss',
})
export class AdminOverviewComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  kpis: KpiCard[] = [
    {
      label: 'Doanh thu hôm nay',
      value: '186,4 tr',
      delta: '↑ 8,2% so với hôm qua',
    },
    {
      label: 'Vé đã phát hành',
      value: '1.284',
      delta: '86% kế hoạch',
    },
    {
      label: 'Phim đang chiếu',
      value: '0',
      delta: 'Đồng bộ từ hệ thống',
    },
    {
      label: 'Cụm rạp hoạt động',
      value: '0',
      delta: 'Toàn hệ thống TheFilm',
    },
  ];

  showtimes: ShowtimeRow[] = [];

  tasks: TaskItem[] = [
    {
      title: 'Duyệt phim mới',
      sub: 'Hồ sơ chờ phê duyệt',
      status: 'MỞ',
    },
    {
      title: 'Hoàn tiền chờ duyệt',
      sub: '02 yêu cầu khách hàng',
      status: 'MỞ',
    },
    {
      title: 'Tồn bắp thấp',
      sub: 'Rạp Royal City',
      status: 'MỞ',
    },
    {
      title: 'Đồng bộ hóa đơn thuế',
      sub: 'Tích hợp tự động',
      status: 'MỞ',
    },
  ];

  chartDays: ChartDay[] = [
    { day: 'T2', height: 34, color: '#c90e29' },
    { day: 'T3', height: 51, color: '#efa8b2' },
    { day: 'T4', height: 44, color: '#c90e29' },
    { day: 'T5', height: 66, color: '#c90e29' },
    { day: 'T6', height: 58, color: '#c90e29' },
    { day: 'T7', height: 82, color: '#c90e29' },
    { day: 'CN', height: 73, color: '#c90e29' },
  ];

  ngOnInit(): void {
    this.loadLiveMetrics();
  }

  loadLiveMetrics(): void {
    this.adminOps.getFilms().subscribe((films) => {
      this.kpis[2].value = `${films.length}`;
      this.kpis[2].delta = `${films.filter((f) => f.approvalStatus === 'APPROVED').length} phim đã duyệt`;
    });

    this.adminOps.getCinemas().subscribe((cinemas) => {
      this.kpis[3].value = `${cinemas.length} rạp`;
      this.kpis[3].delta = `${cinemas[0]?.cinemaName || 'Toàn quốc'}`;
    });

    this.adminOps.getScreenings(1).subscribe((scs) => {
      if (scs.length > 0) {
        this.showtimes = scs.slice(0, 5).map((s) => {
          const start = new Date(s.startsAt);
          const timeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}`;
          return {
            timeMovie: `${timeStr} · ${s.filmTitle || 'Suất chiếu'}`,
            room: s.roomName || 'Phòng 01',
            seats: '108 / 120',
            status: s.status === 'ACTIVE' ? 'Mở bán' : s.status,
            isGreen: s.status === 'ACTIVE',
          };
        });
      }
    });
  }
}
