import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import {
  AdminBackofficeService,
  AdminCatalogService,
  LayoutSeat,
  SeatKind,
} from '@the-film/admin/data-access';
import { formatDateTime } from '../shared/admin-formatters';

type Tool = SeatKind | 'BROKEN';

@Component({
  selector: 'admin-seat-layout',
  standalone: true,
  imports: [RouterLink, NzButtonModule, NzIconModule],
  templateUrl: './seat-layout.component.html',
  styleUrl: './seat-layout.component.scss',
})
export class SeatLayoutComponent {
  private readonly catalog = inject(AdminCatalogService);
  private readonly bo = inject(AdminBackofficeService);
  private readonly message = inject(NzMessageService);

  /** Từ route /cinemas/rooms/:roomId (withComponentInputBinding). */
  readonly roomId = input.required<string>();

  readonly room = computed(() => this.catalog.rooms().find((r) => r.id === Number(this.roomId())) ?? null);
  readonly cinema = computed(() => this.catalog.cinemas().find((c) => c.id === this.room()?.cinemaId) ?? null);

  readonly seats = signal<LayoutSeat[]>([]);
  readonly dirty = signal(false);
  readonly tool = signal<Tool>('VIP');
  readonly tools: { key: Tool; label: string }[] = [
    { key: 'STANDARD', label: 'Ghế thường' },
    { key: 'VIP', label: 'Ghế VIP' },
    { key: 'COUPLE', label: 'Ghế đôi' },
    { key: 'GAP', label: 'Lối đi' },
    { key: 'BROKEN', label: 'Báo hỏng / sửa xong' },
  ];

  constructor() {
    effect(() => {
      const id = Number(this.roomId());
      this.seats.set(this.bo.layoutOf(id).map((s) => ({ ...s })));
      this.dirty.set(false);
    });
  }

  readonly rows = computed(() => {
    const byRow = new Map<string, LayoutSeat[]>();
    for (const s of this.seats()) byRow.set(s.row, [...(byRow.get(s.row) ?? []), s]);
    return [...byRow.entries()].map(([row, seats]) => ({ row, seats }));
  });

  readonly counts = computed(() => {
    const s = this.seats();
    const usable = s.filter((x) => x.kind !== 'GAP');
    return {
      total: usable.length,
      standard: usable.filter((x) => x.kind === 'STANDARD').length,
      vip: usable.filter((x) => x.kind === 'VIP').length,
      couple: usable.filter((x) => x.kind === 'COUPLE').length,
      broken: usable.filter((x) => x.broken).length,
    };
  });

  readonly maintenance = computed(() =>
    this.bo.seatMaintenance.items().filter((m) => m.roomId === Number(this.roomId()))
  );

  readonly formatDateTime = formatDateTime;

  label(s: LayoutSeat): string {
    return `${s.row}${s.col}`;
  }

  paint(target: LayoutSeat): void {
    const tool = this.tool();
    this.seats.update((list) =>
      list.map((s) => {
        if (s !== target) return s;
        if (tool === 'BROKEN') return s.kind === 'GAP' ? s : { ...s, broken: !s.broken };
        return { ...s, kind: tool, broken: tool === 'GAP' ? false : s.broken };
      })
    );
    this.dirty.set(true);
  }

  save(): void {
    const room = this.room();
    if (!room) return;
    const before = new Set(this.bo.layoutOf(room.id).filter((s) => s.broken).map((s) => this.label(s)));
    for (const s of this.seats()) {
      const lbl = this.label(s);
      if (s.broken && !before.has(lbl)) {
        this.bo.seatMaintenance.upsert({
          roomId: room.id,
          cinemaName: this.cinema()?.name ?? '',
          roomName: room.name,
          seatLabel: lbl,
          reason: 'Báo hỏng từ sơ đồ ghế',
          reportedAt: new Date().toISOString(),
          status: 'OPEN',
        });
      }
      if (!s.broken && before.has(lbl)) {
        const open = this.maintenance().find((m) => m.seatLabel === lbl && m.status === 'OPEN');
        if (open) this.bo.seatMaintenance.patch(open.id, { status: 'RESOLVED' });
      }
    }
    this.bo.saveLayout(room.id, this.seats().map((s) => ({ ...s })));
    this.catalog.upsertRoom({ ...room, seatCount: this.counts().total });
    this.dirty.set(false);
    this.message.success('Đã lưu sơ đồ ghế');
  }

  discard(): void {
    this.seats.set(this.bo.layoutOf(Number(this.roomId())).map((s) => ({ ...s })));
    this.dirty.set(false);
  }
}
