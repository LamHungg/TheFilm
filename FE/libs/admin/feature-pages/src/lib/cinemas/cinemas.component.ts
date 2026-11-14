import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzMessageService } from 'ng-zorro-antd/message';
import {
  AdminBackofficeService,
  AdminCatalogService,
  AdminCinema,
  AdminRoom,
} from '@the-film/admin/data-access';
import { statusLabel } from '../shared/admin-formatters';

type Status = AdminCinema['status'];

@Component({
  selector: 'admin-cinemas',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    NzCardModule,
    NzGridModule,
    NzTagModule,
    NzTableModule,
    NzInputModule,
    NzDrawerModule,
    NzButtonModule,
    NzIconModule,
    NzModalModule,
    NzFormModule,
    NzSelectModule,
  ],
  templateUrl: './cinemas.component.html',
  styleUrl: './cinemas.component.scss',
})
export class CinemasComponent {
  readonly statusLabel = statusLabel;
  private readonly catalog = inject(AdminCatalogService);
  private readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);

  readonly keyword = signal('');
  readonly selectedId = signal<number | null>(null);
  readonly drawerOpen = signal(false);
  readonly statuses: Status[] = ['ACTIVE', 'MAINTENANCE', 'INACTIVE'];
  readonly roomTypes = computed(() => this.bo.roomTypes.items().map((t) => t.code));

  readonly cinemas = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    return this.catalog.cinemas().filter(
      (c) =>
        !kw ||
        c.name.toLowerCase().includes(kw) ||
        c.province.toLowerCase().includes(kw) ||
        c.code.toLowerCase().includes(kw)
    );
  });

  readonly selected = computed(() => this.catalog.cinemas().find((c) => c.id === this.selectedId()) ?? null);
  readonly roomsOfSelected = computed(() => {
    const id = this.selectedId();
    return id ? this.catalog.rooms().filter((r) => r.cinemaId === id) : [];
  });

  // Form rạp
  readonly cinemaModal = signal(false);
  cinemaForm: Omit<AdminCinema, 'roomCount' | 'id'> & { id: number | null } = this.blankCinema();

  // Form phòng
  readonly roomModal = signal(false);
  roomForm: Omit<AdminRoom, 'id'> & { id: number | null } = this.blankRoom(0);

  statusColor(status: Status): string {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'MAINTENANCE':
        return 'warning';
      default:
        return 'default';
    }
  }

  openRooms(cinema: AdminCinema): void {
    this.selectedId.set(cinema.id);
    this.drawerOpen.set(true);
  }

  private blankCinema() {
    return { id: null, code: '', name: 'THEFILM ', province: '', address: '', phone: '', status: 'ACTIVE' as Status };
  }

  private blankRoom(cinemaId: number) {
    return { id: null, cinemaId, name: '', roomType: 'STANDARD', seatCount: 120, status: 'ACTIVE' as Status };
  }

  openCinema(cinema?: AdminCinema): void {
    this.cinemaForm = cinema ? { ...cinema, phone: cinema.phone ?? '' } : this.blankCinema();
    this.cinemaModal.set(true);
  }

  saveCinema(): void {
    const f = this.cinemaForm;
    if (!f.code.trim() || !f.name.trim() || !f.province.trim()) {
      this.message.error('Nhập mã, tên và tỉnh / thành của rạp');
      return;
    }
    this.catalog.upsertCinema({ ...f, code: f.code.trim().toUpperCase(), name: f.name.trim() });
    this.message.success('Đã lưu rạp');
    this.cinemaModal.set(false);
  }

  openRoom(room?: AdminRoom): void {
    this.roomForm = room ? { ...room } : this.blankRoom(this.selectedId() ?? 0);
    this.roomModal.set(true);
  }

  saveRoom(): void {
    const f = this.roomForm;
    if (!f.name.trim() || !(Number(f.seatCount) > 0)) {
      this.message.error('Nhập tên phòng và số ghế lớn hơn 0');
      return;
    }
    this.catalog.upsertRoom({ ...f, name: f.name.trim(), seatCount: Number(f.seatCount) });
    this.message.success('Đã lưu phòng chiếu');
    this.roomModal.set(false);
  }
}
