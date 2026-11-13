import { Component, computed, inject } from '@angular/core';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import {
  AdminOpsService,
  AdminSupportCase,
  SupportStatus,
} from '@the-film/admin/data-access';
import { formatDateTime, statusLabel } from '../shared/admin-formatters';

@Component({
  selector: 'admin-support',
  standalone: true,
  imports: [
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzDropdownModule,
    NzIconModule,
  ],
  templateUrl: './support.component.html',
  styleUrl: './support.component.scss',
})
export class SupportComponent {
  readonly statusLabel = statusLabel;
  private readonly ops = inject(AdminOpsService);
  private readonly message = inject(NzMessageService);

  readonly rows = computed(() => this.ops.supportCases());
  readonly formatDateTime = formatDateTime;

  statusColor(status: SupportStatus): string {
    switch (status) {
      case 'OPEN':
        return 'error';
      case 'IN_PROGRESS':
        return 'processing';
      case 'WAITING':
        return 'warning';
      case 'RESOLVED':
      case 'CLOSED':
        return 'success';
      default:
        return 'default';
    }
  }

  priorityColor(priority: AdminSupportCase['priority']): string {
    switch (priority) {
      case 'HIGH':
        return 'magenta';
      case 'MEDIUM':
        return 'gold';
      default:
        return 'default';
    }
  }

  advance(row: AdminSupportCase): void {
    const flow: SupportStatus[] = ['OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED'];
    const idx = flow.indexOf(row.status);
    const next = flow[Math.min(idx + 1, flow.length - 1)];
    this.ops.setSupportStatus(row.id, next);
    this.message.success(`${row.ticketCode} → ${next}`);
  }
}
