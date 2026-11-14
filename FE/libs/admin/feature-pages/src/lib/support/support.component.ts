import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AdminBackofficeService } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, SaveEvent } from '../shared/resource-table/resource-table.component';
import { opts, plain, upsertFrom } from '../shared/crud-helpers';
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
    FormsModule,
    NzTabsModule,
    NzDrawerModule,
    NzInputModule,
    ResourceTableComponent,
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
  readonly bo = inject(AdminBackofficeService);

  readonly thread = signal<AdminSupportCase | null>(null);
  readonly messages = computed(() => this.bo.supportMessages.items().filter((m) => m.caseId === this.thread()?.id).sort((a, b) => a.at.localeCompare(b.at)));
  reply = '';

  openThread(row: AdminSupportCase): void {
    this.reply = '';
    this.thread.set(row);
  }

  sendReply(): void {
    const c = this.thread();
    if (!c || !this.reply.trim()) return;
    // Gửi tới khách (email / chat) do BE nối; ở đây chỉ thêm vào hội thoại.
    this.bo.supportMessages.upsert({ caseId: c.id, from: 'AGENT', author: 'Nguyễn Minh Quân', body: this.reply.trim(), at: new Date().toISOString() });
    this.reply = '';
  }

  readonly articleCols: ColDef[] = [
    { key: 'title', label: 'Tiêu đề', kind: 'strong', sub: 'topic' },
    { key: 'updatedAt', label: 'Cập nhật', kind: 'datetime', width: '170px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];
  readonly articleFields: FieldDef[] = [
    { key: 'title', label: 'Tiêu đề', required: true, wide: true },
    { key: 'topic', label: 'Chủ đề', kind: 'select', options: plain(['Đặt vé', 'Hoàn vé', 'Thành viên', 'Thanh toán', 'Bắp nước']) },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: opts(['DRAFT', 'PUBLISHED']) },
  ];

  saveArticle(e: SaveEvent): void {
    upsertFrom(this.bo.knowledge, { ...e, value: { ...e.value, updatedAt: new Date().toISOString() } });
  }

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
