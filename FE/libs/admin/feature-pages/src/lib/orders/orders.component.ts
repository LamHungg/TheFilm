import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AdminOpsService, AdminOrder, OrderStatus } from '@the-film/admin/data-access';
import { formatDateTime, formatVnd, statusLabel } from '../shared/admin-formatters';

@Component({
  selector: 'admin-orders',
  standalone: true,
  imports: [
    FormsModule,
    NzTableModule,
    NzTagModule,
    NzSelectModule,
    NzInputModule,
    NzDrawerModule,
    NzButtonModule,
    NzDescriptionsModule,
    NzDropdownModule,
    NzIconModule,
  ],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss',
})
export class OrdersComponent {
  readonly statusLabel = statusLabel;
  private readonly ops = inject(AdminOpsService);
  private readonly message = inject(NzMessageService);

  readonly keyword = signal('');
  readonly statusFilter = signal<OrderStatus | 'ALL'>('ALL');
  readonly selected = signal<AdminOrder | null>(null);
  readonly drawerOpen = signal(false);

  readonly statuses: OrderStatus[] = [
    'PENDING_PAYMENT',
    'PAID',
    'CANCELLED',
    'REFUNDED',
    'EXPIRED',
  ];

  readonly rows = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    const status = this.statusFilter();
    return this.ops.orders().filter((o) => {
      const matchKw =
        !kw ||
        o.orderCode.toLowerCase().includes(kw) ||
        o.customerName.toLowerCase().includes(kw) ||
        o.filmTitle.toLowerCase().includes(kw);
      const matchStatus = status === 'ALL' || o.status === status;
      return matchKw && matchStatus;
    });
  });

  readonly formatVnd = formatVnd;
  readonly formatDateTime = formatDateTime;

  statusColor(status: OrderStatus): string {
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

  open(order: AdminOrder): void {
    this.selected.set(order);
    this.drawerOpen.set(true);
  }

  markPaid(order: AdminOrder): void {
    this.ops.setOrderStatus(order.id, 'PAID');
    this.message.success(`Đã đánh dấu ${order.orderCode} là PAID`);
    this.selected.update((o) => (o ? { ...o, status: 'PAID' } : o));
  }

  refund(order: AdminOrder): void {
    this.ops.setOrderStatus(order.id, 'REFUNDED');
    this.message.success(`Đã hoàn ${order.orderCode}`);
    this.selected.update((o) => (o ? { ...o, status: 'REFUNDED' } : o));
  }
}
