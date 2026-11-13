import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import {
  AdminOpsService,
  AdminPromotion,
  PromoStatus,
} from '@the-film/admin/data-access';
import { formatDate, formatVnd, statusLabel } from '../shared/admin-formatters';

@Component({
  selector: 'admin-promotions',
  standalone: true,
  imports: [
    FormsModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzIconModule,
    NzProgressModule,
    NzDropdownModule,
  ],
  templateUrl: './promotions.component.html',
  styleUrl: './promotions.component.scss',
})
export class PromotionsComponent {
  readonly statusLabel = statusLabel;
  private readonly ops = inject(AdminOpsService);
  private readonly message = inject(NzMessageService);

  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly rows = computed(() => this.ops.promotions());

  form = {
    code: '',
    name: '',
    discountType: 'PERCENT' as 'PERCENT' | 'FIXED',
    discountValue: 10,
    startDate: '',
    endDate: '',
    status: 'DRAFT' as PromoStatus,
    usageLimit: 1000,
  };

  readonly statuses: PromoStatus[] = ['DRAFT', 'ACTIVE', 'PAUSED', 'ENDED'];
  readonly formatDate = formatDate;
  readonly formatVnd = formatVnd;

  statusColor(status: PromoStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'PAUSED':
        return 'warning';
      case 'ENDED':
        return 'default';
      default:
        return 'processing';
    }
  }

  usagePercent(row: AdminPromotion): number {
    if (!row.usageLimit) return 0;
    return Math.min(100, Math.round((row.usageCount / row.usageLimit) * 100));
  }

  discountLabel(row: AdminPromotion): string {
    return row.discountType === 'PERCENT'
      ? `${row.discountValue}%`
      : formatVnd(row.discountValue);
  }

  openCreate(): void {
    this.editingId.set(null);
    const today = new Date().toISOString().slice(0, 10);
    this.form = {
      code: '',
      name: '',
      discountType: 'PERCENT',
      discountValue: 10,
      startDate: today,
      endDate: today,
      status: 'DRAFT',
      usageLimit: 1000,
    };
    this.modalOpen.set(true);
  }

  openEdit(row: AdminPromotion): void {
    this.editingId.set(row.id);
    this.form = {
      code: row.code,
      name: row.name,
      discountType: row.discountType,
      discountValue: row.discountValue,
      startDate: row.startDate,
      endDate: row.endDate,
      status: row.status,
      usageLimit: row.usageLimit ?? 0,
    };
    this.modalOpen.set(true);
  }

  save(): void {
    if (!this.form.code.trim() || !this.form.name.trim()) {
      this.message.error('Nhập mã và tên khuyến mãi');
      return;
    }
    this.ops.upsertPromotion({
      id: this.editingId() ?? undefined,
      code: this.form.code.trim().toUpperCase(),
      name: this.form.name.trim(),
      discountType: this.form.discountType,
      discountValue: Number(this.form.discountValue) || 0,
      startDate: this.form.startDate,
      endDate: this.form.endDate,
      status: this.form.status,
      usageLimit: Number(this.form.usageLimit) || undefined,
    });
    this.message.success('Đã lưu khuyến mãi');
    this.modalOpen.set(false);
  }

  toggle(row: AdminPromotion): void {
    const next = row.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    this.ops.setPromotionStatus(row.id, next);
    this.message.success(`Đã chuyển ${row.code} → ${next}`);
  }
}
