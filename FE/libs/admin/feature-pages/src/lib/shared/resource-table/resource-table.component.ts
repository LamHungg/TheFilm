import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import {
  formatDate,
  formatDateTime,
  formatVnd,
  statusColor,
  statusLabel,
} from '../admin-formatters';

export type ColKind =
  | 'text'
  | 'strong'
  | 'code'
  | 'money'
  | 'number'
  | 'percent'
  | 'date'
  | 'datetime'
  | 'tag'
  | 'bool';

export interface ColDef {
  key: string;
  label: string;
  kind?: ColKind;
  /** Dòng phụ xám dưới giá trị chính (chỉ cho kind text/strong). */
  sub?: string;
  width?: string;
  /** Tô màu chữ cho số (vd. tồn kho dưới mức tối thiểu, điểm âm). */
  tone?: (row: any) => 'danger' | 'warn' | 'ok' | null;
}

export type FieldKind = 'text' | 'number' | 'date' | 'time' | 'select' | 'textarea' | 'switch';

export interface FieldDef {
  key: string;
  label: string;
  kind?: FieldKind;
  options?: { value: unknown; label: string }[];
  required?: boolean;
  /** Chiếm cả hai cột của form. */
  wide?: boolean;
  placeholder?: string;
}

export interface RowAction {
  label: string;
  icon: string;
  run: (row: any) => void;
  show?: (row: any) => boolean;
  danger?: boolean;
}

export interface SaveEvent {
  id: number | null;
  value: Record<string, unknown>;
}

/**
 * Bảng danh sách dùng chung cho các trang quản trị dạng CRUD: tìm, lọc theo một cột,
 * thêm / sửa trong modal, xoá có xác nhận, menu thao tác theo dòng.
 */
@Component({
  selector: 'admin-resource-table',
  standalone: true,
  imports: [
    FormsModule,
    NzButtonModule,
    NzDropdownModule,
    NzEmptyModule,
    NzFormModule,
    NzIconModule,
    NzInputModule,
    NzMenuModule,
    NzModalModule,
    NzSelectModule,
    NzSwitchModule,
    NzTableModule,
    NzTagModule,
  ],
  templateUrl: './resource-table.component.html',
  styleUrl: './resource-table.component.scss',
})
export class ResourceTableComponent {
  private readonly message = inject(NzMessageService);
  private readonly modal = inject(NzModalService);

  readonly rows = input.required<any[]>();
  readonly columns = input.required<ColDef[]>();
  /** Không truyền thì bảng chỉ đọc (không nút thêm, không Sửa). */
  readonly fields = input<FieldDef[]>([]);
  readonly entityName = input('bản ghi');
  readonly createLabel = input<string | null>(null);
  readonly searchKeys = input<string[]>([]);
  readonly searchPlaceholder = input('Tìm kiếm…');
  readonly filterKey = input<string | null>(null);
  readonly filterLabel = input('Tất cả');
  readonly actions = input<RowAction[]>([]);
  readonly deletable = input(false);
  readonly emptyText = input('Chưa có dữ liệu');
  readonly pageSize = input(10);
  readonly defaults = input<Record<string, unknown>>({});

  readonly save = output<SaveEvent>();
  readonly remove = output<any>();

  readonly keyword = signal('');
  readonly filterValue = signal<string | null>(null);
  readonly modalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  form: Record<string, any> = {};

  readonly filterOptions = computed(() => {
    const key = this.filterKey();
    if (!key) return [];
    return [...new Set(this.rows().map((r) => String(r[key])))];
  });

  readonly visibleRows = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    const keys = this.searchKeys();
    const fk = this.filterKey();
    const fv = this.filterValue();
    return this.rows().filter(
      (r) =>
        (!fk || !fv || String(r[fk]) === fv) &&
        (!kw || keys.some((k) => String(r[k] ?? '').toLowerCase().includes(kw)))
    );
  });

  /** Bề ngang tối thiểu của bảng: cột có width giữ đúng, cột tự giãn được ít nhất 240px; hẹp hơn thì cuộn ngang. */
  readonly scrollX = computed(() => {
    const cols = this.columns().reduce((sum, c) => sum + (c.width ? parseInt(c.width, 10) : 240), 0);
    return `${cols + (this.hasMenu() ? 56 : 0)}px`;
  });

  readonly hasMenu = computed(
    () => this.fields().length > 0 || this.deletable() || this.actions().length > 0
  );

  readonly statusLabel = statusLabel;
  readonly statusColor = statusColor;

  display(row: any, col: ColDef): string {
    const v = row[col.key];
    if (v === null || v === undefined || v === '') return '—';
    switch (col.kind) {
      case 'money':
        return formatVnd(Number(v));
      case 'number':
        return Number(v).toLocaleString('vi-VN');
      case 'percent':
        return `${Number(v).toLocaleString('vi-VN')}%`;
      case 'date':
        return formatDate(String(v));
      case 'datetime':
        return formatDateTime(String(v));
      case 'bool':
        return v ? 'Có' : 'Không';
      default:
        return String(v);
    }
  }

  isNumeric(col: ColDef): boolean {
    return col.kind === 'money' || col.kind === 'number' || col.kind === 'percent';
  }

  visibleActions(row: any): RowAction[] {
    return this.actions().filter((a) => !a.show || a.show(row));
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = { ...this.defaults() };
    this.modalOpen.set(true);
  }

  openEdit(row: any): void {
    this.editingId.set(row.id);
    this.form = { ...row };
    this.modalOpen.set(true);
  }

  submit(): void {
    const missing = this.fields().find(
      (f) => f.required && (this.form[f.key] === undefined || String(this.form[f.key]).trim() === '')
    );
    if (missing) {
      this.message.error(`Nhập ${missing.label.toLowerCase()}`);
      return;
    }
    const value: Record<string, unknown> = {};
    for (const f of this.fields()) {
      const raw = this.form[f.key];
      value[f.key] = f.kind === 'number' ? Number(raw) || 0 : typeof raw === 'string' ? raw.trim() : raw;
    }
    this.save.emit({ id: this.editingId(), value });
    this.message.success(`Đã lưu ${this.entityName()}`);
    this.modalOpen.set(false);
  }

  confirmRemove(row: any): void {
    this.modal.confirm({
      nzTitle: `Xoá ${this.entityName()} này?`,
      nzContent: 'Thao tác không hoàn tác được.',
      nzOkText: 'Xoá',
      nzOkDanger: true,
      nzCancelText: 'Huỷ',
      nzOnOk: () => {
        this.remove.emit(row);
        this.message.success(`Đã xoá ${this.entityName()}`);
      },
    });
  }
}
