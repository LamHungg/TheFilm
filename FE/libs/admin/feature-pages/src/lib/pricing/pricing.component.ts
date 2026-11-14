import { Component, computed, inject } from '@angular/core';
import { AdminBackofficeService, AdminCatalogService } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, opts, plain, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-pricing',
  standalone: true,
  imports: [ResourceTableComponent],
  template: `
    <p class="page-note">
      Giá vé lấy theo quy tắc khớp có <b>độ ưu tiên cao nhất</b> (rạp, loại phòng, loại ghế, loại ngày, khung giờ).
      Phụ thu loại phòng, loại ghế cộng thêm sau, cấu hình ở trang Loại phòng & ghế.
    </p>
    <admin-resource-table
      [rows]="bo.priceRules.items()"
      [columns]="cols"
      [fields]="fields()"
      entityName="quy tắc giá"
      createLabel="Thêm quy tắc giá"
      [searchKeys]="['name', 'cinemaScope']"
      searchPlaceholder="Tìm theo tên, rạp"
      filterKey="dayType"
      filterLabel="Mọi loại ngày"
      [defaults]="{ status: 'ACTIVE', cinemaScope: 'Toàn chuỗi', dayType: 'WEEKDAY', roomType: 'STANDARD', seatType: 'STANDARD', timeFrom: '08:00', timeTo: '23:59', priority: 10 }"
      [deletable]="true"
      (save)="upsert(bo.priceRules, $event)"
      (remove)="bo.priceRules.remove($event.id)"
    />
  `,
})
export class PricingComponent {
  readonly bo = inject(AdminBackofficeService);
  private readonly catalog = inject(AdminCatalogService);
  readonly upsert = upsertFrom;

  readonly cols: ColDef[] = [
    { key: 'name', label: 'Quy tắc', kind: 'strong', sub: 'cinemaScope' },
    { key: 'dayType', label: 'Loại ngày', kind: 'tag', width: '130px' },
    { key: 'timeFrom', label: 'Từ', width: '80px' },
    { key: 'timeTo', label: 'Đến', width: '80px' },
    { key: 'roomType', label: 'Phòng', kind: 'code', width: '110px' },
    { key: 'seatType', label: 'Ghế', kind: 'code', width: '110px' },
    { key: 'price', label: 'Giá vé', kind: 'money', width: '130px' },
    { key: 'priority', label: 'Ưu tiên', kind: 'number', width: '90px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];

  private readonly scopeOpts = computed(() => plain(['Toàn chuỗi', ...this.catalog.cinemas().map((c) => c.name)]));
  fields(): FieldDef[] {
    return [
      { key: 'name', label: 'Tên quy tắc', required: true, wide: true },
      { key: 'cinemaScope', label: 'Áp dụng cho', kind: 'select', options: this.scopeOpts() },
      { key: 'dayType', label: 'Loại ngày', kind: 'select', options: opts(['WEEKDAY', 'WEEKEND', 'HOLIDAY']) },
      { key: 'roomType', label: 'Loại phòng', kind: 'select', options: plain(this.bo.roomTypes.items().map((r) => r.code)) },
      { key: 'seatType', label: 'Loại ghế', kind: 'select', options: plain(this.bo.seatTypes.items().map((s) => s.code)) },
      { key: 'timeFrom', label: 'Từ giờ', kind: 'time' },
      { key: 'timeTo', label: 'Đến giờ', kind: 'time' },
      { key: 'price', label: 'Giá vé (₫)', kind: 'number', required: true },
      { key: 'priority', label: 'Độ ưu tiên', kind: 'number' },
      { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    ];
  }
}
