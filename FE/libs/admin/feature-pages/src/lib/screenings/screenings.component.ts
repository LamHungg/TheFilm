import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import {
  AdminCatalogService,
  AdminScreening,
  ScreeningStatus,
} from '@the-film/admin/data-access';
import { formatDateTime, formatVnd, statusLabel } from '../shared/admin-formatters';

@Component({
  selector: 'admin-screenings',
  standalone: true,
  imports: [
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzTagModule,
    NzSelectModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzIconModule,
    NzProgressModule,
    NzDropdownModule,
  ],
  templateUrl: './screenings.component.html',
  styleUrl: './screenings.component.scss',
})
export class ScreeningsComponent {
  readonly statusLabel = statusLabel;
  private readonly catalog = inject(AdminCatalogService);
  private readonly message = inject(NzMessageService);
  private readonly modal = inject(NzModalService);

  readonly cinemaFilter = signal<number | 'ALL'>('ALL');
  readonly statusFilter = signal<ScreeningStatus | 'ALL'>('ALL');
  readonly modalOpen = signal(false);

  form = {
    filmId: 1,
    cinemaId: 1,
    roomId: 1,
    startAt: '',
    basePrice: 100000,
    status: 'SCHEDULED' as ScreeningStatus,
  };

  readonly films = this.catalog.films;
  readonly cinemas = this.catalog.cinemas;
  readonly rooms = computed(() => this.catalog.roomsByCinema(this.form.cinemaId));
  readonly statuses: ScreeningStatus[] = [
    'SCHEDULED',
    'ON_SALE',
    'SOLD_OUT',
    'CANCELLED',
    'COMPLETED',
  ];

  readonly rows = computed(() => {
    const cinemaId = this.cinemaFilter();
    const status = this.statusFilter();
    return this.catalog.screenings().filter((s) => {
      const matchCinema = cinemaId === 'ALL' || s.cinemaId === cinemaId;
      const matchStatus = status === 'ALL' || s.status === status;
      return matchCinema && matchStatus;
    });
  });

  readonly formatDateTime = formatDateTime;
  readonly formatVnd = formatVnd;

  statusColor(status: ScreeningStatus): string {
    switch (status) {
      case 'ON_SALE':
        return 'success';
      case 'SCHEDULED':
        return 'processing';
      case 'SOLD_OUT':
        return 'warning';
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  }

  fillPercent(row: AdminScreening): number {
    return Math.round((row.soldSeats / row.totalSeats) * 100);
  }

  openCreate(): void {
    const film = this.films()[0];
    const cinema = this.cinemas()[0];
    const room = this.catalog.roomsByCinema(cinema.id)[0];
    const start = new Date();
    start.setHours(start.getHours() + 3, 0, 0, 0);
    this.form = {
      filmId: film.id,
      cinemaId: cinema.id,
      roomId: room.id,
      startAt: start.toISOString().slice(0, 16),
      basePrice: 100000,
      status: 'SCHEDULED',
    };
    this.modalOpen.set(true);
  }

  onCinemaChange(cinemaId: number): void {
    this.form.cinemaId = cinemaId;
    const room = this.catalog.roomsByCinema(cinemaId)[0];
    this.form.roomId = room?.id ?? 0;
  }

  save(): void {
    const film = this.films().find((f) => f.id === this.form.filmId);
    const cinema = this.cinemas().find((c) => c.id === this.form.cinemaId);
    const room = this.catalog
      .roomsByCinema(this.form.cinemaId)
      .find((r) => r.id === this.form.roomId);
    if (!film || !cinema || !room) {
      this.message.error('Thiếu thông tin phim/rạp/phòng');
      return;
    }
    const start = new Date(this.form.startAt);
    const end = new Date(start.getTime() + film.durationMin * 60_000);
    this.catalog.upsertScreening({
      filmId: film.id,
      filmTitle: film.title,
      cinemaId: cinema.id,
      cinemaName: cinema.name,
      roomId: room.id,
      roomName: room.name,
      startAt: start.toISOString(),
      endAt: end.toISOString(),
      status: this.form.status,
      basePrice: Number(this.form.basePrice) || 0,
      soldSeats: 0,
      totalSeats: room.seatCount,
    });
    this.message.success('Đã tạo suất chiếu');
    this.modalOpen.set(false);
  }

  cancelScreening(row: AdminScreening): void {
    this.modal.confirm({
      nzTitle: 'Hủy suất chiếu này?',
      nzContent: `${row.filmTitle} · ${row.cinemaName}`,
      nzOkText: 'Hủy suất',
      nzOkDanger: true,
      nzCancelText: 'Đóng',
      nzOnOk: () => {
        this.catalog.setScreeningStatus(row.id, 'CANCELLED');
        this.message.success('Đã hủy suất chiếu');
      },
    });
  }

  remove(row: AdminScreening): void {
    this.modal.confirm({
      nzTitle: 'Xoá suất chiếu?',
      nzContent: 'Suất sẽ bị gỡ khỏi lịch mock.',
      nzOkText: 'Xoá',
      nzOkDanger: true,
      nzCancelText: 'Huỷ',
      nzOnOk: () => {
        this.catalog.deleteScreening(row.id);
        this.message.success('Đã xoá suất chiếu');
      },
    });
  }
}
