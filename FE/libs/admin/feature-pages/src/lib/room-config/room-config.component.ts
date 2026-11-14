import { Component, inject } from '@angular/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { AdminBackofficeService, SeatMaintenance } from '@the-film/admin/data-access';
import { ColDef, FieldDef, ResourceTableComponent, RowAction } from '../shared/resource-table/resource-table.component';
import { ACTIVE_OPTS, upsertFrom } from '../shared/crud-helpers';

@Component({
  selector: 'admin-room-config',
  standalone: true,
  imports: [NzTabsModule, ResourceTableComponent],
  template: `
    <nz-tabs class="page-tabs">
      <nz-tab nzTitle="Bảo trì ghế">
        <admin-resource-table
          [rows]="bo.seatMaintenance.items()"
          [columns]="maintCols"
          [actions]="maintActions"
          entityName="phiếu bảo trì"
          [searchKeys]="['cinemaName', 'roomName', 'seatLabel', 'reason']"
          searchPlaceholder="Tìm theo rạp, phòng, ghế"
          filterKey="status"
          filterLabel="Mọi trạng thái"
          emptyText="Không có ghế nào đang bảo trì"
        />
      </nz-tab>
      <nz-tab nzTitle="Loại phòng">
        <admin-resource-table
          [rows]="bo.roomTypes.items()"
          [columns]="roomCols"
          [fields]="roomFields"
          entityName="loại phòng"
          createLabel="Thêm loại phòng"
          [defaults]="{ status: 'ACTIVE', surcharge: 0 }"
          (save)="upsert(bo.roomTypes, $event)"
        />
      </nz-tab>
      <nz-tab nzTitle="Loại ghế">
        <admin-resource-table
          [rows]="bo.seatTypes.items()"
          [columns]="seatCols"
          [fields]="seatFields"
          entityName="loại ghế"
          createLabel="Thêm loại ghế"
          [defaults]="{ status: 'ACTIVE', surcharge: 0, seatsPerUnit: 1 }"
          (save)="upsert(bo.seatTypes, $event)"
        />
      </nz-tab>
    </nz-tabs>
  `,
})
export class RoomConfigComponent {
  readonly bo = inject(AdminBackofficeService);
  readonly upsert = upsertFrom;

  readonly maintCols: ColDef[] = [
    { key: 'seatLabel', label: 'Ghế', kind: 'strong', sub: 'roomName', width: '140px' },
    { key: 'cinemaName', label: 'Rạp' },
    { key: 'reason', label: 'Lý do' },
    { key: 'reportedAt', label: 'Báo lúc', kind: 'datetime', width: '170px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '130px' },
  ];
  readonly maintActions: RowAction[] = [
    {
      label: 'Đánh dấu đã sửa xong',
      icon: 'check',
      show: (r: SeatMaintenance) => r.status === 'OPEN',
      run: (r: SeatMaintenance) => this.bo.seatMaintenance.patch(r.id, { status: 'RESOLVED' }),
    },
  ];

  readonly roomCols: ColDef[] = [
    { key: 'name', label: 'Loại phòng', kind: 'strong', sub: 'description' },
    { key: 'code', label: 'Mã', kind: 'code', width: '120px' },
    { key: 'surcharge', label: 'Phụ thu / vé', kind: 'money', width: '150px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  readonly roomFields: FieldDef[] = [
    { key: 'name', label: 'Tên', required: true },
    { key: 'code', label: 'Mã', required: true },
    { key: 'surcharge', label: 'Phụ thu mỗi vé (₫)', kind: 'number' },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
    { key: 'description', label: 'Mô tả', kind: 'textarea' },
  ];

  readonly seatCols: ColDef[] = [
    { key: 'name', label: 'Loại ghế', kind: 'strong' },
    { key: 'code', label: 'Mã', kind: 'code', width: '120px' },
    { key: 'seatsPerUnit', label: 'Số chỗ / ghế', kind: 'number', width: '130px' },
    { key: 'surcharge', label: 'Phụ thu / vé', kind: 'money', width: '150px' },
    { key: 'status', label: 'Trạng thái', kind: 'tag', width: '140px' },
  ];
  readonly seatFields: FieldDef[] = [
    { key: 'name', label: 'Tên', required: true },
    { key: 'code', label: 'Mã', required: true },
    { key: 'seatsPerUnit', label: 'Số chỗ mỗi ghế', kind: 'number' },
    { key: 'surcharge', label: 'Phụ thu mỗi vé (₫)', kind: 'number' },
    { key: 'status', label: 'Trạng thái', kind: 'select', options: ACTIVE_OPTS },
  ];
}
