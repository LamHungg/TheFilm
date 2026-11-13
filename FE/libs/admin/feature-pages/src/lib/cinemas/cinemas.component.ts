import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AdminCatalogService, AdminCinema } from '@the-film/admin/data-access';
import { statusLabel } from '../shared/admin-formatters';

@Component({
  selector: 'admin-cinemas',
  standalone: true,
  imports: [
    FormsModule,
    NzCardModule,
    NzGridModule,
    NzTagModule,
    NzTableModule,
    NzInputModule,
    NzDrawerModule,
    NzButtonModule,
    NzIconModule,
  ],
  templateUrl: './cinemas.component.html',
  styleUrl: './cinemas.component.scss',
})
export class CinemasComponent {
  readonly statusLabel = statusLabel;
  private readonly catalog = inject(AdminCatalogService);

  readonly keyword = signal('');
  readonly selected = signal<AdminCinema | null>(null);
  readonly drawerOpen = signal(false);

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

  roomsOfSelected = computed(() => {
    const cinema = this.selected();
    return cinema ? this.catalog.roomsByCinema(cinema.id) : [];
  });

  statusColor(status: AdminCinema['status']): string {
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
    this.selected.set(cinema);
    this.drawerOpen.set(true);
  }
}
